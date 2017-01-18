import ajax from 'ajax';
import form from 'src/modules/form/helper/formUtil';
import validity from 'src/modules/form/validation/validity';
import serializer from 'src/modules/form/helper/serializer';
import utilsHelper from 'src/utils/helper';

export function open(){
    //When a payment method "overlay" is opened.
    console.log('start the spinner');
}
export function close(){
    //When a payment method overlay is closed.
    console.log('stop the spinner');
}

// Validates the form; returns true if the form is valid, false otherwise.
export function validateForm() {

    form.elems.map(function (elem) {
        validity.check(elem);
    });

    let formHasErrors = form.errs.length > 0;
    return !formHasErrors;

}

// Creates the new member by posting the form data with the provided token object.
export function postForm (paymentToken) {

    let data = serializer(utilsHelper.toArray(form.elem.elements),
        paymentToken);

    ajax({
        url: form.elem.action,
        method: 'post',
        data: data,
        success: function (successData) {
            window.location.assign(successData.redirect);
        },
        error: function (errData) {
            alert(err);
        }
    });

}
