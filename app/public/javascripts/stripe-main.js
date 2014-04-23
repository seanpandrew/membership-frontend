/*global require */

require.config({
    paths: {
        // 'config': '../app/config',
        'jquery': '//pasteup.guim.co.uk/js/lib/jquery/1.8.1/jquery.min',
        'payment': 'lib/jquery.payment',
        'stripe': 'https://js.stripe.com/v2/?'
    },
    shim: {
        'payment': {
            deps: ['jquery']
        }
    }
});

require([
    'jquery',
    'payment',
    'stripe',
    'gu_u'
], function($, Payment, Stripe, GU_U) {

    'use strict';

    var config = {
        stripePublishableKey: 'pk_test_Qm3CGRdrV4WfGYCpm0sftR0f'
    };

    var stripeResponseHandler = function (status, response) {
        var $form = $('#payment-form');

        if (response.error) {
            // Show the errors on the form
            $form.find('.payment-errors').text(response.error.message);
            $form.find('button').prop('disabled', false);
        } else {
            // token contains id, last4, and card type
            var token = response.id;
            // Insert the token into the form so it gets submitted to the server
            $form.append($('<input type="hidden" name="stripeToken" />').val(token));
            // and submit
            $form.get(0).submit();
        }
    };

    var bindFormatting = function ($form) {
        $form.find('.cc-num').payment('formatCardNumber');
        $form.find('.cc-cvc').payment('formatCardCVC');
        $form.find('.cc-exp-month').payment('restrictNumeric');
        $form.find('.cc-exp-year').payment('restrictNumeric');

    };

    var validateForm = function ($form) {

        var number = $form.find('.cc-num');
        var cvc = $form.find('.cc-cvc');
        var expiryMonth = $form.find('.cc-exp-month');
        var expiryYear = $form.find('.cc-exp-year');

        $form.find('.invalid').removeClass('invalid');

        if (!$.payment.validateCardNumber(number.val())) {
            number.addClass('invalid');
            return false;
        }

        if (!$.payment.validateCardCVC(cvc.val(), $.payment.cardType(number.val()))) {
            cvc.addClass('invalid');
            return false;
        }

        if (!$.payment.validateCardExpiry(expiryMonth.val(), expiryYear.val())) {
            expiryMonth.addClass('invalid');
            expiryYear.addClass('invalid');
            return false;
        }

        return true;
    };

    var populateUserInformation = function () {
        var user = GU_U.getUserFromCookie();

        var str = [];
        str.push('User from cookie - ');
        str.push('\r\ndisplayname: ' + user.displayname);
        str.push('\r\nid: ' + user.id);
        str.push('\r\nemail: ' + user.primaryemailaddress);

        $('p.user').append(str.join(''));
    };

    var init = function () {

        populateUserInformation();

        var $form = $('#payment-form');

        bindFormatting($form);

        Stripe.setPublishableKey(config.stripePublishableKey);

        $form.submit(function(event) {
            event.preventDefault();

            // Disable the submit button to prevent repeated clicks
            $form.find('button').prop('disabled', true);
            if (validateForm($form)) {
                Stripe.card.createToken($form, stripeResponseHandler);
            } else {
                $form.find('button').prop('disabled', false);
            }

        });
    };

    $(init);
});
