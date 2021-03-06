define([
    '$',
    'bean',
    'src/modules/form/billingPeriodChoice',
    'src/modules/form/validation/display',
    'src/modules/form/helper/formUtil'
], function (
    $,
    bean,
    billingPeriodChoice,
    validationDisplay,
    form
) {
    'use strict';

    var CURRENCY_ATTR = 'data-currency';
    var PAYMENT_OPTIONS_CONTAINER_EL = $('.js-payment-options-container')[0];
    var DELIVERY_COUNTRY_EL = $('#country-deliveryAddress')[0];
    var BILLING_COUNTRY_EL = $('#country-billingAddress')[0];
    var BILLING_ADDRESS_SEL = $('.js-billingAddress-fieldset');
    var BILLING_ADDRESS_EL = BILLING_ADDRESS_SEL[0];
    var USE_BILLING_ADDRESS_EL = $('#use-billing-address')[0];
    var USE_DELIVERY_ADDRESS_EL = $('#use-delivery-address')[0];
    var BILLING_CTA_SEL = $('.js-toggle-billing-address');
    var FRIEND_FORM_EL = $('.js-friend-form')[0];

    var checkoutForm = guardian.membership.checkoutForm;

    /**
     * One way binding for the model object in 'guardian.membership.checkoutForm'.
     * This object is generated by the backend and mutated as the user interacts with the form.
     */

    function renderPrices() {
        billingPeriodChoice.render();
        hideBillingAddress();
        selectDeliveryCountry();
    }

    function renderTermsAndConditions() {
        var usLegalSelector = $('.us-legal');
        var territory = territoryFromCountry(checkoutForm.billingCountry)
        if (usLegalSelector[0]) {
            switch (territory) {
                case 'US':
                    usLegalSelector[0].classList.remove('is-hidden');
                    break;
                default :
                    usLegalSelector[0].classList.add('is-hidden');
            }
        }

        localiseTermsAndConditionsLinks(territory);

    }

    function localiseTermsAndConditionsLinks(territory) {
        var formTermsLinkSelector = $('.ts-and-cs a'),
            footerTermsLinkSelector = $('#qa-footer-link-terms'),
            termsLinks = {
                'UK': 'https://www.theguardian.com/info/2014/sep/09/guardian-membership-terms-and-conditions',
                'US': 'https://www.theguardian.com/info/2016/nov/08/guardian-members-us-terms-and-conditions',
                'AU': 'https://www.theguardian.com/info/2016/nov/08/guardian-members-australia-terms-and-conditions',
                'INT': 'https://www.theguardian.com/info/2016/nov/08/guardian-members-international-terms-and-conditions'
            };

        if (formTermsLinkSelector) {
            for (var i = 0; i < formTermsLinkSelector.length; i++) {
                var elText = formTermsLinkSelector[i].text.toLowerCase();
                if (elText === 'terms & conditions' || elText === 'terms and conditions') {
                    formTermsLinkSelector[i].href = termsLinks[territory];
                }
            }
        }
        if (footerTermsLinkSelector[0]) {
            footerTermsLinkSelector[0].href = termsLinks[territory];
        }
    }

    function territoryFromCountry(country) {
        switch (country) {
            case 'GB':
                return 'UK';
            case 'AU':
                return country;
            case 'US':
            case 'UM':
            case 'VI':
                return 'US';
            default:
                return 'INT';
        }
    }

    function hasParent(elem) {
        return elem.parentNode;
    }

    function hideBillingAddress() {
        if (!hasParent(BILLING_ADDRESS_EL) && checkoutForm.showBillingAddress) {
            BILLING_ADDRESS_SEL.insertAfter(BILLING_CTA_SEL);
        } else if (hasParent(BILLING_ADDRESS_EL) && !checkoutForm.showBillingAddress){
            validationDisplay.resetErrorState($('[required]', BILLING_ADDRESS_SEL));
            BILLING_ADDRESS_SEL.detach();
        }
    }

    function selectDeliveryCountry() {
        selectCountry(DELIVERY_COUNTRY_EL, checkoutForm.deliveryCountry);
        if (!checkoutForm.showBillingAddress && checkoutForm.selectedBillingCountry !== checkoutForm.billingCountry) {
            selectCountry(BILLING_COUNTRY_EL, checkoutForm.billingCountry);
            bean.fire(BILLING_COUNTRY_EL, 'change');
        }
    }

    function selectCountry(selectEl, country) {
        $('option', selectEl).each(function (el) {
            if ($(el).val() === country) {
                $(el)[0].selected = true;
            }
        });
    }

    function init() {
        checkoutForm.deliveryCountry = checkoutForm.defaultCountry;

        if (PAYMENT_OPTIONS_CONTAINER_EL && checkoutForm) {
            checkoutForm.showBillingAddress = false;
            checkoutForm.billingCountry = checkoutForm.deliveryCountry;
            addListeners();
            renderPrices();
        }

        if (FRIEND_FORM_EL && checkoutForm) {
            selectCountry(DELIVERY_COUNTRY_EL, checkoutForm.deliveryCountry);
        }

        if (DELIVERY_COUNTRY_EL) {
            bean.fire(DELIVERY_COUNTRY_EL, 'change');
        }
    }

    function addListeners() {
        bean.on(DELIVERY_COUNTRY_EL, 'change', function(e) {
            var input = e.target;
            var selectedCountry = $(input).val();

            checkoutForm.deliveryCountry = selectedCountry;
            if (!checkoutForm.showBillingAddress) {
                checkoutForm.billingCountry = selectedCountry;
            }

            renderPrices();
        });

        bean.on(BILLING_COUNTRY_EL, 'change', function(e) {
            var input = e.target;
            var selectedCountry = $(input).val();
            var selectedEl = toArray($('option', input)).filter(function (el) {
                return $(el).val() === selectedCountry;
            })[0];
            var selectedCurrency = $(selectedEl).attr(CURRENCY_ATTR);

            // handles the case where the initial choice is blank,
            // e.g. when you are a european supporter
            if (selectedCurrency) {
                checkoutForm.currency = selectedCurrency;
            }
            checkoutForm.selectedBillingCountry = selectedCountry;
            checkoutForm.billingCountry = selectedCountry;

            renderTermsAndConditions();
            renderPrices();
        });

        bean.on(USE_BILLING_ADDRESS_EL, 'click', function() {
            checkoutForm.showBillingAddress = true;

            renderPrices();
            form.flush();
        });

        bean.on(USE_DELIVERY_ADDRESS_EL, 'click', function() {
            checkoutForm.showBillingAddress = false;
            checkoutForm.billingCountry = checkoutForm.deliveryCountry;

            renderPrices();
            form.flush();
        });
    }

    function toArray(nodeList) {
        return Array.prototype.slice.call(nodeList);
    }

    return {
        init: init
    };
});
