package com.intelliacc.MLKitBarcodeScanner;

// ----------------------------------------------------------------------------
// |  Android Imports
// ----------------------------------------------------------------------------
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

// ----------------------------------------------------------------------------
// |  Google Imports
// ----------------------------------------------------------------------------
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.material.snackbar.Snackbar;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;

// ----------------------------------------------------------------------------
// |  Java Imports
// ----------------------------------------------------------------------------
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// ----------------------------------------------------------------------------
// |  Our Imports
// ----------------------------------------------------------------------------
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.intelliacc.MLKitBarcodeScanner.camera.MLKitCameraSource2;
import com.intelliacc.MLKitBarcodeScanner.camera.MLKitCameraSourcePreview;
import com.intelliacc.MLKitBarcodeScanner.camera.MLKitGraphicOverlay;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;

public final class MLKitBarcodeCaptureActivity extends    AppCompatActivity
                                          implements MLKitBarcodeScanningProcessor.BarcodeUpdateListener {
  // ----------------------------------------------------------------------------
  // |  Public Properties
  // ----------------------------------------------------------------------------
  public              Integer DetectionTypes                            ;
  public              double  ViewFinderWidth  = .5                     ;
  public              double  ViewFinderHeight = .7                     ;
  public static final String  BarcodeValue     = "FirebaseVisionBarcode";
  public              Integer CameraFacing     = 1                      ;
  public              String  PromptText       = "";

  // ----------------------------------------------------------------------------
  // |  Private Properties
  // ----------------------------------------------------------------------------
  private static final String TAG                   = "Barcode-reader";
  private static final int    RC_HANDLE_GMS         = 9001            ;
  private static final int    RC_HANDLE_CAMERA_PERM = 2               ;

  private MLKitCameraSource2                  _CameraSource        ;
  private MLKitCameraSourcePreview            _Preview             ;
  private MLKitGraphicOverlay<MLKitBarcodeGraphic> _GraphicOverlay      ;
  private ScaleGestureDetector           _ScaleGestureDetector;
  private GestureDetector                _GestureDetector     ;
  private ImageView                      _ScanningLine        ;
  private ObjectAnimator                 _ScanningAnimator    ;
  private TextView                       _PromptText          ;

  public List<String> globalBarcodes = new ArrayList<String>();
  // ----------------------------------------------------------------------------
  // |  Public Functions
  // ----------------------------------------------------------------------------
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    // Make status bar transparent
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow().setStatusBarColor(Color.TRANSPARENT);
      getWindow().getDecorView().setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
      );
    } else {
      // For older versions, just hide the status bar
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    // Hide the action bar
    if (getActionBar() != null) {
      getActionBar().hide();
    }

    if (getSupportActionBar() != null) {
      getSupportActionBar().hide();
    }

    setContentView(getResources().getIdentifier("mlkit_barcode_capture", "layout", getPackageName()));
    setupUI(findViewById(getResources().getIdentifier("topLayout", "id", getPackageName())));

    _Preview = (MLKitCameraSourcePreview) findViewById(getResources().getIdentifier("preview", "id", getPackageName()));
    _Preview.ViewFinderWidth = ViewFinderWidth;
    _Preview.ViewFinderHeight = ViewFinderHeight;
    _GraphicOverlay = (MLKitGraphicOverlay<MLKitBarcodeGraphic>) findViewById(getResources().getIdentifier("graphicOverlay", "id", getPackageName()));

    // Setup scanning animation
    _ScanningLine = (ImageView) findViewById(getResources().getIdentifier("scanningLine", "id", getPackageName()));
    setupScanningAnimation();

    // read parameters from the intent used to launch the activity.
    DetectionTypes = getIntent().getIntExtra("DetectionTypes", 1234);
    ViewFinderWidth = getIntent().getDoubleExtra("ViewFinderWidth", .5);
    ViewFinderHeight = getIntent().getDoubleExtra("ViewFinderHeight", .7);
    CameraFacing = getIntent().getIntExtra("CameraFacing", 1);
    PromptText = getIntent().getStringExtra("PromptText");
    Log.d(TAG, "BarcodeCaptureActivity -> CameraFacing = " + CameraFacing);
    Log.d(TAG, "BarcodeCaptureActivity -> PromptText = " + PromptText);

    // Setup prompt text after we have the text from intent
    _PromptText = (TextView) findViewById(getResources().getIdentifier("promptText", "id", getPackageName()));
    if (_PromptText != null) {
        _PromptText.setText(PromptText != null ? PromptText : "");
        _PromptText.setVisibility(View.VISIBLE);
        Log.d(TAG, "Prompt text view found and text set to: " + PromptText);
    } else {
        Log.e(TAG, "Prompt text view not found!");
    }

    EditText txtBarcode = (EditText) findViewById(getResources().getIdentifier("txtBarcode", "id", getPackageName()));
    Button clickButton = (Button) findViewById(getResources().getIdentifier("submitBarcode", "id", getPackageName()));
    clickButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onBarcodeDetected(String.valueOf(txtBarcode.getText()));
      }
    });

    txtBarcode.setOnKeyListener(new View.OnKeyListener() {
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
          switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
              onBarcodeDetected(String.valueOf(txtBarcode.getText()));
              return true;
            default:
              break;
          }
        }
        return false;
      }
    });

    int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
    if (rc == PackageManager.PERMISSION_GRANTED) {
      createCameraSource(true, false, CameraFacing);
    } else {
      requestCameraPermission();
    }

    _GestureDetector = new GestureDetector(this, new CaptureGestureListener());
    _ScaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
  }

  public static void hideSoftKeyboard(Activity activity) {
    InputMethodManager inputMethodManager =
      (InputMethodManager) activity.getSystemService(
        Activity.INPUT_METHOD_SERVICE);
    if(inputMethodManager.isAcceptingText()){
      inputMethodManager.hideSoftInputFromWindow(
        activity.getCurrentFocus().getWindowToken(),
        0
      );
    }
  }

  public void setupUI(View view) {

    // Set up touch listener for non-text box views to hide keyboard.
    if (!(view instanceof EditText)) {
      view.setOnTouchListener(new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
          hideSoftKeyboard(MLKitBarcodeCaptureActivity.this);
          return false;
        }
      });
    }

    //If a layout container, iterate over children and seed recursion.
    if (view instanceof ViewGroup) {
      for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
        View innerView = ((ViewGroup) view).getChildAt(i);
        setupUI(innerView);
      }
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    boolean b = _ScaleGestureDetector.onTouchEvent(e);
    boolean c = _GestureDetector.onTouchEvent(e);

    return b || c || super.onTouchEvent(e);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode != RC_HANDLE_CAMERA_PERM) {
      Log.d(TAG, "Got unexpected permission result: " + requestCode);
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
      return;
    }

    if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      Log.d(TAG, "Camera permission granted - initialize the camera source");
      // we have permission, so create the camerasource
      DetectionTypes = getIntent().getIntExtra("DetectionTypes", 0);
      ViewFinderWidth = getIntent().getDoubleExtra("ViewFinderWidth", .5);
      ViewFinderHeight = getIntent().getDoubleExtra("ViewFinderHeight", .7);
      CameraFacing = getIntent().getIntExtra("CameraFacing", 1);
      PromptText = getIntent().getStringExtra("PromptText");
      Log.d(TAG, "onRequestPermissionsResult -> CameraFacing = " + CameraFacing);

      createCameraSource(true, false, CameraFacing);
      return;
    }

    Log.e(TAG, "Permission not granted: results len = " + grantResults.length + " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        finish();
      }
    };

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Camera permission required")
        .setMessage(getResources().getIdentifier("no_camera_permission", "string", getPackageName()))
        .setPositiveButton(getResources().getIdentifier("ok", "string", getPackageName()), listener).show();
  }

  @Override
  public void onBarcodeDetected(String barcode) {
    // do something with barcode data returned
    if(globalBarcodes.size() < 2) {
      globalBarcodes.add(barcode);
    }
    if(globalBarcodes.size() > 1) {
      Intent data = new Intent();
      if(globalBarcodes.get(0).equals(globalBarcodes.get(1))) {
        data.putExtra(BarcodeValue, barcode);
        setResult(CommonStatusCodes.SUCCESS, data);
      } else {
        data.putExtra(BarcodeValue, "BARCODE_NOT_MATCH");
        setResult(CommonStatusCodes.ERROR, data);
      }

      finish();
    }
  }

  // ----------------------------------------------------------------------------
  // |  Protected Functions
  // ----------------------------------------------------------------------------
  @Override
  protected void onResume() {
    super.onResume();
    startCameraSource();
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (_Preview != null) {
      _Preview.stop();
    }
    if (_ScanningAnimator != null) {
      _ScanningAnimator.cancel();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (_Preview != null) {
      _Preview.release();
    }
  }

  // ----------------------------------------------------------------------------
  // |  Private Functions
  // ----------------------------------------------------------------------------
  @SuppressLint("InlinedApi")
  private void createCameraSource(boolean autoFocus, boolean useFlash, Integer cameraFacing) {
    BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
  		.setBarcodeFormats(
  		  Barcode.FORMAT_ALL_FORMATS
      )
      .build();

    BarcodeScanner scanner = BarcodeScanning.getClient(options);
    MLKitBarcodeScanningProcessor scanningProcessor = new MLKitBarcodeScanningProcessor(scanner, this);

    Log.d(TAG, "createCameraSource -> CameraFacing = " + cameraFacing);
    Log.d(TAG, "createCameraSource -> CameraFacing type = " + (cameraFacing != null ? cameraFacing.getClass().getName() : "null"));

    MLKitCameraSource2.Builder builder = new MLKitCameraSource2.Builder(getApplicationContext(), scanningProcessor)
        .setFacing(cameraFacing != null ? cameraFacing : 1) // Ensure we have a default value
        .setRequestedPreviewSize(1600, 1024)
        .setRequestedFps(30.0f);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      builder = builder.setFocusMode(autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null);
    }

    _CameraSource = builder.setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null).build();
  }

  private void startCameraSource() throws SecurityException {
    int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
    if (code != ConnectionResult.SUCCESS) {
      Dialog dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
      dlg.show();
    }

    if (_CameraSource != null) {
      try {
        Log.d(TAG, "Starting camera source with facing: " + CameraFacing);
        _Preview.start(_CameraSource, _GraphicOverlay);
      } catch (IOException e) {
        Log.e(TAG, "Unable to start camera source.", e);
        _CameraSource.release();
        _CameraSource = null;
      }
    }
  }

  private void requestCameraPermission() {
    Log.w(TAG, "Camera permission is not granted. Requesting permission");

    final String[] permissions = new String[] { Manifest.permission.CAMERA };

    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
      ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
      return;
    }

    final Activity thisActivity = this;

    View.OnClickListener listener = new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        ActivityCompat.requestPermissions(thisActivity, permissions, RC_HANDLE_CAMERA_PERM);
      }
    };

    findViewById(getResources().getIdentifier("topLayout", "id", getPackageName())).setOnClickListener(listener);
    Snackbar
        .make(_GraphicOverlay, getResources().getIdentifier("permission_camera_rationale", "string", getPackageName()),
            Snackbar.LENGTH_INDEFINITE)
        .setAction(getResources().getIdentifier("ok", "string", getPackageName()), listener).show();
  }

  // ----------------------------------------------------------------------------
  // |  Helper classes
  // ----------------------------------------------------------------------------
  private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
      return super.onSingleTapConfirmed(e);
    }
  }

  private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
      return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
      return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
      _CameraSource.doZoom(detector.getScaleFactor());
    }
  }

  private void setupScanningAnimation() {
    // Get the screen height to calculate animation range
    int screenHeight = getResources().getDisplayMetrics().heightPixels;

    // Calculate the animation range (center of screen ± 100dp)
    float centerY = screenHeight / 2f;
    float range = dpToPx(100); // 100dp range to cover a bit less than the full height of the scanning frame

    // Ensure the scanning line is visible and centered
    _ScanningLine.setVisibility(View.VISIBLE);
    _ScanningLine.bringToFront();

    // Set initial position to center
    _ScanningLine.setTranslationY(0);

    // Create and start the animation
    _ScanningAnimator = ObjectAnimator.ofFloat(_ScanningLine, "translationY",
        -range, range);
    _ScanningAnimator.setDuration(1000); // 2 seconds for one complete cycle
    _ScanningAnimator.setRepeatCount(ValueAnimator.INFINITE);
    _ScanningAnimator.setRepeatMode(ValueAnimator.REVERSE);
    _ScanningAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

    // Start the animation after a short delay
    _ScanningLine.post(new Runnable() {
        @Override
        public void run() {
            _ScanningAnimator.start();
        }
    });
  }

  private int dpToPx(int dp) {
    float density = getResources().getDisplayMetrics().density;
    return Math.round((float) dp * density);
  }
}
