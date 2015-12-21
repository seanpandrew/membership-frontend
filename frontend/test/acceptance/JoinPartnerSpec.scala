package acceptance

import acceptance.util._
import org.scalatest.selenium.WebBrowser
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, FeatureSpec, GivenWhenThen}
import org.slf4j.LoggerFactory

class JoinPartnerSpec extends FeatureSpec
  with WebBrowser with WebBrowserUtil
  with GivenWhenThen with BeforeAndAfter with BeforeAndAfterAll  {

  def logger = LoggerFactory.getLogger(this.getClass)

  before { // Before each test
    resetDriver()
  }

  override def beforeAll() = {
    Screencast.storeId()
    Config.printSummary()
  }

  override def afterAll() = {
    Config.driver.quit()
  }

  private def assumeDependenciesAreAvailable() = {
    assume(Dependencies.MembershipFrontend.isAvailable,
      s"- ${Dependencies.MembershipFrontend.url} unavaliable! " +
        "\nPlease run local membership-frontend server before running tests.")

    assume(Dependencies.IdentityFrontend.isAvailable,
      s"- ${Dependencies.IdentityFrontend.url} unavaliable! " +
        "\nPlease run local identity-frontend server before running tests.")
  }

  feature("Become a Partner in UK") {
    scenario("I join as Partner by clicking 'Become a Partner' button on Membership homepage", Acceptance) {
      assumeDependenciesAreAvailable()

      val testUser = new TestUser

      Given("I clicked 'Become a Partner' button on Membership homepage")

      When("I land on 'Identity Register' page")
      val register = new pages.Register(testUser)
      go.to(register)
      assert(register.pageHasLoaded())

      And("I fill in personal details")
      register.fillInPersonalDetails()

      And("I submit the form to create my Identity account")
      register.submit()

      Then("I should land on 'Enter Details' page")
      val enterDetails = new pages.EnterDetails
      assert(enterDetails.pageHasLoaded())

      And("I should have Identity cookies")
      Seq("GU_U", "SC_GU_U", "SC_GU_LA").foreach { idCookie =>
        assert(cookiesSet.map(_.getName).contains(idCookie)) }

      And("I should be logged in with my Identity account.")
      assert(enterDetails.userDisplayName.toLowerCase == testUser.username.toLowerCase)

      When("I fill in delivery address details")
      enterDetails.fillInDeliveryAddress()

      And("I fill in card details")
      enterDetails.fillInCardDetails()

      And("I click 'Pay' button")
      enterDetails.pay()

      Then("I should land on 'Thank You' page")
      val thankYou = new pages.ThankYou
      assert(thankYou.pageHasLoaded())

      And("I should be signed in as Partner.")
      assert(thankYou.userDisplayName.toLowerCase == testUser.username.toLowerCase)
      assert(thankYou.userTier == "Partner")
    }
  }
}

