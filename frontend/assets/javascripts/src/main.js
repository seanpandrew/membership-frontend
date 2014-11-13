require([
    '$',
    'lib/bower-components/imager.js/Imager',
    'src/utils/analytics/setup',
    'src/utils/cookieRefresh',
    'ajax',
    'src/modules/tier/JoinFree',
    'src/modules/info/Feedback',
    'src/modules/tier/PaidForm',
    'src/modules/events/Cta',
    'src/modules/Header',
    'src/modules/UserDetails',
    'src/modules/tier/Choose',
    'src/modules/events/eventPriceEnhance',
    'src/modules/tier/Thankyou',
    'lib/bower-components/raven-js/dist/raven', // add new deps ABOVE this
    'src/utils/modernizr'
], function(
    $,
    Imager,
    analytics,
    cookieRefresh,
    ajax,
    JoinFree,
    FeedbackForm,
    PaidForm,
    Cta,
    Header,
    UserDetails,
    Choose,
    eventPriceEnhance,
    Thankyou
) {
    'use strict';

    /*global Raven */
    // set up Raven, which speaks to Sentry to track errors
    Raven.config('https://e159339ea7504924ac248ba52242db96@app.getsentry.com/29912', {
        whitelistUrls: ['membership.theguardian.com/assets/'],
        tags: { build_number: guardian.membership.buildNumber }
    }).install();

    ajax.init({page: {ajaxUrl: ''}});

    /* jshint ignore:start */
    // avoid "Do not use 'new' for side effects" error
    // event imagery
    if ($('.delayed-image-load').length) {
        new Imager('.delayed-image-load', {
            availableWidths: guardian.membership.eventImages.widths,
            availablePixelRatios: guardian.membership.eventImages.ratios
        });
    }
    // home page hero (a-b) imagery
    if ($('.delayed-home-image-load').length) {
        new Imager('.delayed-home-image-load', {
            availableWidths: guardian.membership.homeImages.widths,
            availablePixelRatios: guardian.membership.homeImages.ratios
        });
    }
    // home page promo (a-b) imagery
    if ($('.delayed-home-promo-image-load').length) {
        new Imager('.delayed-home-promo-image-load', {
            availableWidths: guardian.membership.homeImages.promoWidths,
            availablePixelRatios: guardian.membership.homeImages.ratios
        });
    }
    /* jshint ignore:end */

    // TODO: Remove this, see module
    cookieRefresh.init();

    analytics.init();

    // Global
    var header = new Header();
    header.init();
    (new UserDetails()).init();

    // Events
    (new Cta()).init();
    eventPriceEnhance.init();

    // Join
    (new Choose()).init();
    (new JoinFree()).init();
    (new PaidForm()).init();
    (new Thankyou()).init(header);

    // Feedback
    (new FeedbackForm()).init();
});
