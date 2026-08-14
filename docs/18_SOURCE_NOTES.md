# Current Platform and Tax-System Source Notes

These notes are provided to anchor development decisions as of **14 August 2026**. Re-verify before release.

## Android / Google Play

- Android 16 is API level 36.
- Google Play states that starting **31 August 2026**, new apps and app updates must target Android 16 / API level 36 or higher.
- Therefore this pack uses `compileSdk 36` and `targetSdk 36` as the baseline.

Official references:
- https://developer.android.com/about/versions/16/
- https://developer.android.com/tools/releases/platforms
- https://support.google.com/googleplay/android-developer/answer/11926878

## Pakistan FBR / IRIS

FBR's IRIS system is the official electronic environment for income-tax return and wealth-statement filing. FBR's filing guidance notes that wealth-statement reconciliation is part of successful submission.

Official references:
- https://iris.fbr.gov.pk/
- https://www.fbr.gov.pk/categ/file-income-tax-return/
- https://help.fbr.gov.pk/

Search of official FBR material during preparation of this pack surfaced API documentation for **Digital Invoicing**, but did not establish a public general-purpose API for third-party consumer apps to submit an individual's annual income-tax return directly.

Therefore MVP architecture intentionally:
- prepares a complete return-ready dataset;
- exports a tax preparation pack;
- does not store FBR login credentials;
- does not automate/scrape IRIS;
- does not claim direct official filing.

Any future FBR submission adapter must be based on a then-current, documented and authorized official interface.
