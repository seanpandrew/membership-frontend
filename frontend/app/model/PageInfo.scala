package model

import views.support.Asset
import configuration.{Config, CopyConfig}

case class PageInfo(
  title: String,
  url: String,
  description: Option[String],
  image: Option[String] = Some(PageInfo.defaultImage),
  schemaOpt: Option[EventSchema] = None,
  stripePublicKey: Option[String] = None,
  customSignInUrl: Option[String] = None,
  hasBackgroundImage: Boolean = true
)

object PageInfo {
  val defaultImage = Config.membershipUrl + Asset.at("images/common/mem-promo.jpg")

  // url has the domain prepended in templates
  val default = PageInfo(
    title=CopyConfig.copyTitleDefault,
    url="/",
    description=Some(CopyConfig.copyDescriptionDefault)
  )
}
