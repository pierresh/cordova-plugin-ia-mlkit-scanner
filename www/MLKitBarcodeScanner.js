var exec = require('cordova/exec');

var PLUGIN_NAME = 'MLKitBarcodeScanner';

var MLKitBarcodeScanner = {
	/** Open the barcode scanning interface to scan one barcode, then return the result. */
	scanBarcode: function (options) {
		return new Promise((resolve, reject) => {
			options = options || {};
			options.prompt = options.prompt || "";

			exec(
				(result) => {
					resolve(result);
				},
				(error) => {
					reject(error);
				},
				"MLKitBarcodeScanner",
				"scanBarcode",
				[options.cameraFacing || 1, options.viewFinderWidth || 0.5, options.viewFinderHeight || 0.7, options.detectionTypes || 1234, options.prompt]
			);
		});
	},
	/** Check if Google Play Services is available. Android ONLY. */
	checkSupport: function (success, failure) {
		if (!success) { success = (_) => { }; }
		if (!failure) { failure = (_) => { }; }

		exec(success, failure, PLUGIN_NAME, "checkSupport", []);
	}
}

module.exports = MLKitBarcodeScanner;