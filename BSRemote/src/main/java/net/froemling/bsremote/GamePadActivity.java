package net.froemling.bsremote;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import android.annotation.SuppressLint;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.SystemClock;
import android.provider.Settings.Secure;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import java.lang.Math;

class MyGLSurfaceView extends GLSurfaceView {

  private int _keyPickUp;
  private int _keyJump;
  private int _keyPunch;
  private int _keyBomb;
  private int _keyRun1;
  private int _keyRun2;
  private int _keyStart;

  private final GLRenderer _gl;
  private final GamePadActivity _gamePadActivity;

  int _dPadTouch = -1;
  int _prefsTouch = -1;
  int _quitTouch = -1;
  int _startTouch = -1;
  boolean _prefsHover;
  boolean _quitHover;
  boolean _startHover;

  boolean _dPadTouchIsMove;
  boolean _inited = false;
  float _buttonCenterX;
  float _buttonCenterY;
  float _dPadCenterX;
  float _dPadCenterY;

  float _dPadScale;
  float _buttonScale;
  float _dPadOffsX;
  float _dPadOffsY;
  float _buttonOffsX;
  float _buttonOffsY;
  float _dPadTouchStartX;
  float _dPadTouchStartY;

  float _sizeMin = 0.5f;
  float _sizeMax = 1.5f;

  float _dPadOffsXMin = -0.25f;
  float _dPadOffsXMax = 1.0f;
  float _dPadOffsYMin = -1.0f;
  float _dPadOffsYMax = 0.8f;

  float _buttonOffsXMin = -0.8f;
  float _buttonOffsXMax = 0.25f;
  float _buttonOffsYMin = -1.0f;
  float _buttonOffsYMax = 0.8f;

  String _dPadType;

  public MyGLSurfaceView(Context context) {
    super(context);

    _gamePadActivity = (GamePadActivity) context;

    setFocusable(true);
    setFocusableInTouchMode(true);
    requestFocus();

    // Create an OpenGL ES 2.0 context.
    setEGLContextClientVersion(2);

    // Set the Renderer for drawing on the GLSurfaceView
    _gl = new GLRenderer(_gamePadActivity.getApplicationContext());

    // this seems to be necessary in the emulator..
    if (Build.FINGERPRINT.startsWith("generic")) {
      setEGLConfigChooser(8, 8, 8, 8, 16, 0);
    }
    setRenderer(_gl);

    // Render the view only when there is a change in the drawing data
    setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

    SharedPreferences preferences = _gamePadActivity
        .getSharedPreferences("BSRemotePrefs", Context.MODE_PRIVATE);
    _dPadScale = preferences.getFloat("scale", 1.0f);
    _buttonScale = preferences.getFloat("buttonScale", 1.0f);
    _dPadScale = preferences.getFloat("dPadScale", 1.0f);
    _dPadOffsX = preferences.getFloat("dPadOffsX", 0.4f);
    _dPadOffsY = preferences.getFloat("dPadOffsY", 0.0f);
    _buttonOffsX = preferences.getFloat("buttonOffsX", -0.2f);
    _buttonOffsY = preferences.getFloat("buttonOffsY", 0.0f);
    _dPadType = preferences.getString("dPadType", "floating");
    assert _dPadType != null;
    if (!(_dPadType.equals("floating") || _dPadType.equals("fixed"))) {
      _dPadType = "floating";
    }

    _keyPickUp = preferences.getInt("keyPickUp", KeyEvent.KEYCODE_BUTTON_Y);
    _keyJump = preferences.getInt("keyJump", KeyEvent.KEYCODE_BUTTON_A);
    _keyPunch = preferences.getInt("keyPunch", KeyEvent.KEYCODE_BUTTON_X);
    _keyBomb = preferences.getInt("keyBomb", KeyEvent.KEYCODE_BUTTON_B);
    _keyRun1 = preferences.getInt("keyRun1", KeyEvent.KEYCODE_BUTTON_L1);
    _keyRun2 = preferences.getInt("keyRun2", KeyEvent.KEYCODE_BUTTON_R1);
    _keyStart = preferences.getInt("keyStart", KeyEvent.KEYCODE_BUTTON_START);

  }

  public void onClosing() {
    // if we've got a dialog open, kill it
    if (mPrefsDialog != null && mPrefsDialog.isShowing()) {
      mPrefsDialog.cancel();
    }
  }

  private void _savePrefs() {
    // save this to prefs
    SharedPreferences preferences = _gamePadActivity
        .getSharedPreferences("BSRemotePrefs", Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putFloat("buttonScale", _buttonScale);
    editor.putFloat("dPadScale", _dPadScale);
    editor.putFloat("dPadOffsX", _dPadOffsX);
    editor.putFloat("dPadOffsY", _dPadOffsY);
    editor.putFloat("buttonOffsX", _buttonOffsX);
    editor.putFloat("buttonOffsY", _buttonOffsY);
    editor.putString("dPadType", _dPadType);

    editor.putInt("keyPickUp", _keyPickUp);
    editor.putInt("keyJump", _keyJump);
    editor.putInt("keyPunch", _keyPunch);
    editor.putInt("keyBomb", _keyBomb);
    editor.putInt("keyRun1", _keyRun1);
    editor.putInt("keyRun2", _keyRun2);
    editor.putInt("keyStart", _keyStart);

    editor.apply();
  }

  private void _updateSizes() {

    // update button positions and whatnot
    float ratio = (float) getWidth() / getHeight();
    float height = 1.0f / ratio;

    float bWidth = 0.08f * _buttonScale;
    float bHeight = 0.08f * _buttonScale;
    _buttonCenterX = 0.95f - 0.1f * _buttonScale + _buttonOffsX * 0.2f;
    _buttonCenterY = height * 0.6f - 0.0f * _buttonScale - _buttonOffsY * 0.3f;
    _dPadCenterX = 0.0f + 0.1f * _dPadScale + _dPadOffsX * 0.2f;
    _dPadCenterY = height * 0.6f - 0.0f * _dPadScale - _dPadOffsY * 0.3f;
    float bSep = 0.1f * _buttonScale;

    _gl.quitButtonX = 0.06f * _buttonScale;
    _gl.quitButtonY = 0.035f * _buttonScale;
    _gl.quitButtonWidth = 0.1f * _buttonScale;
    _gl.quitButtonHeight = 0.05f * _buttonScale;

    _gl.prefsButtonX = 0.17f * _buttonScale;
    _gl.prefsButtonY = 0.035f * _buttonScale;
    _gl.prefsButtonWidth = 0.1f * _buttonScale;
    _gl.prefsButtonHeight = 0.05f * _buttonScale;

    _gl.startButtonX = 0.28f * _buttonScale;
    _gl.startButtonY = 0.035f * _buttonScale;
    _gl.startButtonWidth = 0.1f * _buttonScale;
    _gl.startButtonHeight = 0.05f * _buttonScale;

    _gl.throwButtonX = _buttonCenterX;
    _gl.throwButtonY = _buttonCenterY - bSep;
    _gl.throwButtonWidth = bWidth;
    _gl.throwButtonHeight = bHeight;

    _gl.punchButtonX = _buttonCenterX - bSep;
    _gl.punchButtonY = _buttonCenterY;
    _gl.punchButtonWidth = bWidth;
    _gl.punchButtonHeight = bHeight;

    _gl.bombButtonX = _buttonCenterX + bSep;
    _gl.bombButtonY = _buttonCenterY;
    _gl.bombButtonWidth = bWidth;
    _gl.bombButtonHeight = bHeight;

    _gl.jumpButtonX = _buttonCenterX;
    _gl.jumpButtonY = _buttonCenterY + bSep;
    _gl.jumpButtonWidth = bWidth;
    _gl.jumpButtonHeight = bHeight;

    _gl.joystickCenterX = _dPadCenterX;
    _gl.joystickCenterY = _dPadCenterY;
    _gl.joystickWidth = 0.2f * _dPadScale;
    _gl.joystickHeight = 0.2f * _dPadScale;

    _gl.joystickX = _dPadCenterX;
    _gl.joystickY = _dPadCenterY;

    if (!_inited) {
      _inited = true;
    }
  }

  public void onSizeChanged(int w, int h, int oldw, int oldh) {
    _updateSizes();
  }

  private boolean _pointInBox(float x, float y, float bx, float by, float sx,
                              float sy) {
    return x >= bx - 0.5f * sx && x <= bx + 0.5 * sx && y >= by - 0.5 * sy &&
        y <= by + 0.5 * sy;
  }

  void _updateButtonsForTouches(MotionEvent event) {

    boolean punchHeld = false;
    boolean throwHeld = false;
    boolean jumpHeld = false;
    boolean bombHeld = false;
    float mult = 1.0f / getWidth();
    final int pointerCount = event.getPointerCount();

    for (int i = 0; i < pointerCount; i++) {

      // ignore touch-up events
      int actionPointerIndex = event.getActionIndex();
      int action = event.getActionMasked();
      if ((action == MotionEvent.ACTION_UP ||
          action == MotionEvent.ACTION_POINTER_UP) && i == actionPointerIndex) {
        continue;
      }

      int touch = event.getPointerId(i);

      // ignore dpad touch
      if (touch == _dPadTouch) {
        continue;
      }

      float s = 4.0f / _buttonScale;
      // get the point in button-center-coords
      float x = event.getX(i) * mult;
      float y = event.getY(i) * mult;
      float bx = (x - _buttonCenterX) * s;
      float by = (y - _buttonCenterY) * s;

      float threshold = 0.3f;
      float pbx, pby;
      float len;
      float punchLen, jumpLen, throwLen, bombLen;

      // punch
      pbx = (x - _gl.punchButtonX) * s;
      pby = (y - _gl.punchButtonY) * s;
      punchLen = len = (float) Math.sqrt(pbx * pbx + pby * pby);
      if (len < threshold) {
        punchHeld = true;
      }

      // throw
      pbx = (x - _gl.throwButtonX) * s;
      pby = (y - _gl.throwButtonY) * s;
      throwLen = len = (float) Math.sqrt(pbx * pbx + pby * pby);
      if (len < threshold) {
        throwHeld = true;
      }

      // jump
      pbx = (x - _gl.jumpButtonX) * s;
      pby = (y - _gl.jumpButtonY) * s;
      jumpLen = len = (float) Math.sqrt(pbx * pbx + pby * pby);
      if (len < threshold) {
        jumpHeld = true;
      }

      // bomb
      pbx = (x - _gl.bombButtonX) * s;
      pby = (y - _gl.bombButtonY) * s;
      bombLen = len = (float) Math.sqrt(pbx * pbx + pby * pby);
      if (len < threshold) {
        bombHeld = true;
      }

      // how much larger than the button/dpad areas we should count touch
      // events in
      float buttonBuffer = 2.0f;
      // ok now lets take care of fringe areas and non-moved touches
      // a touch in our button area should *always* affect at least one
      // button
      // ..so lets find the closest button && press it.
      // this will probably coincide with what we just set above but thats
      // ok.
      if (x > 0.5 && bx > -1.0 * buttonBuffer && bx < 1.0 * buttonBuffer &&
          by > -1.0 * buttonBuffer && by < 1.0 * buttonBuffer) {
        if (punchLen < throwLen && punchLen < jumpLen && punchLen < bombLen) {
          punchHeld = true;
        } else if (throwLen < punchLen && throwLen < jumpLen &&
            throwLen < bombLen) {
          throwHeld = true;
        } else if (jumpLen < punchLen && jumpLen < throwLen &&
            jumpLen < bombLen) {
          jumpHeld = true;
        } else {
          bombHeld = true;
        }
      }
    }

    boolean throwWasHeld = ((_gamePadActivity._buttonStateV1 &
        GamePadActivity.BS_REMOTE_STATE_THROW) != 0);
    boolean jumpWasHeld = ((_gamePadActivity._buttonStateV1 &
        GamePadActivity.BS_REMOTE_STATE_JUMP) != 0);
    boolean punchWasHeld = ((_gamePadActivity._buttonStateV1 &
        GamePadActivity.BS_REMOTE_STATE_PUNCH) != 0);
    boolean bombWasHeld = ((_gamePadActivity._buttonStateV1 &
        GamePadActivity.BS_REMOTE_STATE_BOMB) != 0);

    // send press events for non-held ones we're now over
    if (!throwWasHeld && throwHeld) {
      _handleThrowPress();
    }
    if (throwWasHeld && !throwHeld) {
      _handleThrowRelease();
    }

    // send press events for non-held ones we're now over
    if ((!punchWasHeld) && punchHeld) {
      _handlePunchPress();
    }
    if (punchWasHeld && !punchHeld) {
      _handlePunchRelease();
    }

    // send press events for non-held ones we're now over
    if ((!bombWasHeld) && bombHeld) {
      _handleBombPress();
    }
    if (bombWasHeld && !bombHeld) {
      _handleBombRelease();
    }

    // send press events for non-held ones we're now over
    if ((!jumpWasHeld) && jumpHeld) {
      _handleJumpPress();
    }
    if (jumpWasHeld && !jumpHeld) {
      _handleJumpRelease();
    }

  }

  private void _handleMenuPress() {
    _gamePadActivity._buttonStateV1 |= GamePadActivity.BS_REMOTE_STATE_MENU;
    _gamePadActivity._buttonStateV2 |= GamePadActivity.BS_REMOTE_STATE2_MENU;

    _gamePadActivity._doStateChange(false);
    _gl.startButtonPressed = true;
  }

  private void _handleMenuRelease() {
    _gamePadActivity._buttonStateV1 &= ~GamePadActivity.BS_REMOTE_STATE_MENU;
    _gamePadActivity._buttonStateV2 &= ~GamePadActivity.BS_REMOTE_STATE2_MENU;
    _gamePadActivity._doStateChange(false);
    _gl.startButtonPressed = false;
  }

  private void _handleRunPress() {
    _gamePadActivity._buttonStateV2 |= GamePadActivity.BS_REMOTE_STATE2_RUN;
    _gamePadActivity._doStateChange(false);
  }

  private void _handleRunRelease() {
    _gamePadActivity._buttonStateV2 &= ~GamePadActivity.BS_REMOTE_STATE2_RUN;
    _gamePadActivity._doStateChange(false);
  }

  private void _handleThrowPress() {
    _gamePadActivity._buttonStateV1 |= GamePadActivity.BS_REMOTE_STATE_THROW;
    _gamePadActivity._buttonStateV2 |= GamePadActivity.BS_REMOTE_STATE2_THROW;
    _gamePadActivity._doStateChange(false);
    _gl.throwPressed = true;
  }

  private void _handleThrowRelease() {
    _gamePadActivity._buttonStateV1 &= ~GamePadActivity.BS_REMOTE_STATE_THROW;
    _gamePadActivity._buttonStateV2 &= ~GamePadActivity.BS_REMOTE_STATE2_THROW;
    _gamePadActivity._doStateChange(false);
    _gl.throwPressed = false;
  }

  private void _handleBombPress() {
    _gamePadActivity._buttonStateV1 |= GamePadActivity.BS_REMOTE_STATE_BOMB;
    _gamePadActivity._buttonStateV2 |= GamePadActivity.BS_REMOTE_STATE2_BOMB;
    _gamePadActivity._doStateChange(false);
    _gl.bombPressed = true;
  }

  private void _handleBombRelease() {
    _gamePadActivity._buttonStateV1 &= ~GamePadActivity.BS_REMOTE_STATE_BOMB;
    _gamePadActivity._buttonStateV2 &= ~GamePadActivity.BS_REMOTE_STATE2_BOMB;
    _gamePadActivity._doStateChange(false);
    _gl.bombPressed = false;
  }

  private void _handleJumpPress() {
    _gamePadActivity._buttonStateV1 |= GamePadActivity.BS_REMOTE_STATE_JUMP;
    _gamePadActivity._buttonStateV2 |= GamePadActivity.BS_REMOTE_STATE2_JUMP;
    _gamePadActivity._doStateChange(false);
    _gl.jumpPressed = true;
  }

  private void _handleJumpRelease() {
    _gamePadActivity._buttonStateV1 &= ~GamePadActivity.BS_REMOTE_STATE_JUMP;
    _gamePadActivity._buttonStateV2 &= ~GamePadActivity.BS_REMOTE_STATE2_JUMP;
    _gamePadActivity._doStateChange(false);
    _gl.jumpPressed = false;
  }

  private void _handlePunchPress() {
    _gamePadActivity._buttonStateV1 |= GamePadActivity.BS_REMOTE_STATE_PUNCH;
    _gamePadActivity._buttonStateV2 |= GamePadActivity.BS_REMOTE_STATE2_PUNCH;
    _gamePadActivity._doStateChange(false);
    _gl.punchPressed = true;
  }

  private void _handlePunchRelease() {
    _gamePadActivity._buttonStateV1 &= ~GamePadActivity.BS_REMOTE_STATE_PUNCH;
    _gamePadActivity._buttonStateV2 &= ~GamePadActivity.BS_REMOTE_STATE2_PUNCH;
    _gamePadActivity._doStateChange(false);
    _gl.punchPressed = false;
  }

  private Dialog mPrefsDialog;

  public void doPrefs() {

    final Dialog d = new Dialog(_gamePadActivity);
    mPrefsDialog = d;

    d.setContentView(R.layout.prefs);
    d.setCanceledOnTouchOutside(true);

    SeekBar seekbar;
    seekbar = d.findViewById(R.id.seekBarButtonSize);
    seekbar.setProgress(
        (int) (100.0f * (_buttonScale - _sizeMin) / (_sizeMax - _sizeMin)));
    seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
      public void onProgressChanged(SeekBar seekBar, int progress,
                                    boolean fromUser) {
        _buttonScale = _sizeMin + (_sizeMax - _sizeMin) * (progress / 100.0f);
        _updateSizes();
        requestRender();
      }

      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      public void onStopTrackingTouch(SeekBar seekBar) {
        _savePrefs();
      }
    });
    seekbar = d.findViewById(R.id.seekBarDPadSize);
    seekbar.setProgress(
        (int) (100.0f * (_dPadScale - _sizeMin) / (_sizeMax - _sizeMin)));
    seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
      public void onProgressChanged(SeekBar seekBar, int progress,
                                    boolean fromUser) {
        _dPadScale = _sizeMin + (_sizeMax - _sizeMin) * (progress / 100.0f);
        _updateSizes();
        requestRender();
      }

      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      public void onStopTrackingTouch(SeekBar seekBar) {
        _savePrefs();
      }
    });
    seekbar = d.findViewById(R.id.seekBarButtonPosition1);
    seekbar.setProgress((int) (100.0f * (_buttonOffsX - _buttonOffsXMin) /
        (_buttonOffsXMax - _buttonOffsXMin)));
    seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
      public void onProgressChanged(SeekBar seekBar, int progress,
                                    boolean fromUser) {
        _buttonOffsX = _buttonOffsXMin +
            (_buttonOffsXMax - _buttonOffsXMin) * (progress / 100.0f);
        _updateSizes();
        requestRender();
      }

      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      public void onStopTrackingTouch(SeekBar seekBar) {
        _savePrefs();
      }
    });
    seekbar = d.findViewById(R.id.seekBarButtonPosition2);
    seekbar.setProgress((int) (100.0f * (_buttonOffsY - _buttonOffsYMin) /
        (_buttonOffsYMax - _buttonOffsYMin)));
    seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
      public void onProgressChanged(SeekBar seekBar, int progress,
                                    boolean fromUser) {
        _buttonOffsY = _buttonOffsYMin +
            (_buttonOffsYMax - _buttonOffsYMin) * (progress / 100.0f);
        _updateSizes();
        requestRender();
      }

      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      public void onStopTrackingTouch(SeekBar seekBar) {
        _savePrefs();
      }
    });
    seekbar = d.findViewById(R.id.seekBarDPadPosition1);
    seekbar.setProgress((int) (100.0f * (_dPadOffsX - _dPadOffsXMin) /
        (_dPadOffsXMax - _dPadOffsXMin)));
    seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
      public void onProgressChanged(SeekBar seekBar, int progress,
                                    boolean fromUser) {
        _dPadOffsX = _dPadOffsXMin +
            (_dPadOffsXMax - _dPadOffsXMin) * (progress / 100.0f);
        _updateSizes();
        requestRender();
      }

      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      public void onStopTrackingTouch(SeekBar seekBar) {
        _savePrefs();
      }
    });
    seekbar = d.findViewById(R.id.seekBarDPadPosition2);
    seekbar.setProgress((int) (100.0f * (_dPadOffsY - _dPadOffsYMin) /
        (_dPadOffsYMax - _dPadOffsYMin)));
    seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
      public void onProgressChanged(SeekBar seekBar, int progress,
                                    boolean fromUser) {
        _dPadOffsY = _dPadOffsYMin +
            (_dPadOffsYMax - _dPadOffsYMin) * (progress / 100.0f);
        _updateSizes();
        requestRender();
      }

      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      public void onStopTrackingTouch(SeekBar seekBar) {
        _savePrefs();
      }
    });

    RadioButton dPadFloatingButton =
        d.findViewById(R.id.radioButtonDPadFloating);
    RadioButton dPadFixedButton = d.findViewById(R.id.radioButtonDPadFixed);

    if (_dPadType.equals("floating")) {
      dPadFloatingButton.setChecked(true);
    } else {
      dPadFixedButton.setChecked(true);
    }

    dPadFloatingButton.setOnClickListener(new RadioButton.OnClickListener() {
      @Override
      public void onClick(View v) {
        _dPadType = "floating";
        _savePrefs();
      }
    });
    dPadFixedButton.setOnClickListener(new RadioButton.OnClickListener() {
      @Override
      public void onClick(View v) {
        _dPadType = "fixed";
        _savePrefs();
      }
    });

    Button configHardwareButton =
        d.findViewById(R.id.buttonConfigureHardwareButtons);
    configHardwareButton.setOnClickListener(new Button.OnClickListener() {
      @Override
      public void onClick(View v) {
        // kill this dialog and bring up the hardware one
        d.cancel();
        doHardwareControlsPrefs();
      }
    });

    d.setTitle(R.string.settings);
    d.show();


  }

  public String getPrettyKeyName(int keyCode) {
    String val = KeyEvent.keyCodeToString(keyCode);
    if (val.startsWith("KEYCODE_")) {
      val = val.replaceAll("KEYCODE_", "");
    }
    if (val.startsWith("BUTTON_")) {
      val = val.replaceAll("BUTTON_", "");
    }
    return val;
  }

  public Dialog doCaptureKey() {

    final Dialog d = new Dialog(_gamePadActivity);
    d.setContentView(R.layout.prefs_capture_key);
    d.setCanceledOnTouchOutside(true);
    d.setOnKeyListener(new Dialog.OnKeyListener() {
      @Override
      public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
        _setActionKey(keyCode);
        d.dismiss();
        return true;
      }
    });

    Button b = d.findViewById(R.id.buttonResetToDefault);
    b.setOnClickListener(new Button.OnClickListener() {
      @Override
      public void onClick(View v) {
        switch (_captureKey) {
          case PICK_UP:
            _setActionKey(KeyEvent.KEYCODE_BUTTON_Y);
            break;
          case JUMP:
            _setActionKey(KeyEvent.KEYCODE_BUTTON_A);
            break;
          case PUNCH:
            _setActionKey(KeyEvent.KEYCODE_BUTTON_X);
            break;
          case BOMB:
            _setActionKey(KeyEvent.KEYCODE_BUTTON_B);
            break;
          case RUN1:
            _setActionKey(KeyEvent.KEYCODE_BUTTON_L1);
            break;
          case RUN2:
            _setActionKey(KeyEvent.KEYCODE_BUTTON_R1);
            break;
          case START:
            _setActionKey(KeyEvent.KEYCODE_BUTTON_START);
            break;
          default:
            LogThread.log("Error: unrecognized key in doActionKey", null);
            break;
        }
        d.dismiss();
      }
    });

    d.setTitle(R.string.capturing);
    d.show();
    return d;
  }

  enum CaptureKey {
    NONE, PICK_UP, JUMP, PUNCH, BOMB, RUN1, RUN2, START
  }

  private CaptureKey _captureKey = CaptureKey.NONE;

  private void _setActionKey(int keyval) {
    switch (_captureKey) {
      case PICK_UP:
        _keyPickUp = keyval;
        break;
      case JUMP:
        _keyJump = keyval;
        break;
      case PUNCH:
        _keyPunch = keyval;
        break;
      case BOMB:
        _keyBomb = keyval;
        break;
      case RUN1:
        _keyRun1 = keyval;
        break;
      case RUN2:
        _keyRun2 = keyval;
        break;
      case START:
        _keyStart = keyval;
        break;
      default:
        LogThread.log("Error: unrecognized key in _setActionKey", null);
        break;
    }
    _savePrefs();
  }

  private void _updateHardwareControlsLabels(Dialog d) {
    TextView t = (TextView) d.findViewById(R.id.textPickUp);
    t.setText(getPrettyKeyName(_keyPickUp));
    t = (TextView) d.findViewById(R.id.textJump);
    t.setText(getPrettyKeyName(_keyJump));
    t = (TextView) d.findViewById(R.id.textPunch);
    t.setText(getPrettyKeyName(_keyPunch));
    t = (TextView) d.findViewById(R.id.textBomb);
    t.setText(getPrettyKeyName(_keyBomb));
    t = (TextView) d.findViewById(R.id.textRun1);
    t.setText(getPrettyKeyName(_keyRun1));
    t = (TextView) d.findViewById(R.id.textRun2);
    t.setText(getPrettyKeyName(_keyRun2));
    t = (TextView) d.findViewById(R.id.textStart);
    t.setText(getPrettyKeyName(_keyStart));

  }

  public void doHardwareControlsPrefs() {

    final Dialog d = new Dialog(_gamePadActivity);
    d.setContentView(R.layout.prefs_hardware_controls);
    d.setCanceledOnTouchOutside(true);
    Button.OnClickListener l = new Button.OnClickListener() {
      @Override
      public void onClick(View v) {

        // take note of which action this capture will apply to, then launch
        // a capture..
        if (v == d.findViewById(R.id.buttonPickUp)) {
          _captureKey = CaptureKey.PICK_UP;
        } else if (v == d.findViewById(R.id.buttonJump)) {
          _captureKey = CaptureKey.JUMP;
        } else if (v == d.findViewById(R.id.buttonPunch)) {
          _captureKey = CaptureKey.PUNCH;
        } else if (v == d.findViewById(R.id.buttonBomb)) {
          _captureKey = CaptureKey.BOMB;
        } else if (v == d.findViewById(R.id.buttonRun1)) {
          _captureKey = CaptureKey.RUN1;
        } else if (v == d.findViewById(R.id.buttonRun2)) {
          _captureKey = CaptureKey.RUN2;
        } else if (v == d.findViewById(R.id.buttonStart)) {
          _captureKey = CaptureKey.START;
        } else {
          LogThread.log("Error: unrecognized capture button", null);
        }

        Dialog d2 = doCaptureKey();
        d2.setOnDismissListener(new Dialog.OnDismissListener() {
          @Override
          public void onDismiss(DialogInterface dialog) {
            _updateHardwareControlsLabels(d);
          }
        });

      }
    };

    d.findViewById(R.id.buttonPickUp).setOnClickListener(l);
    d.findViewById(R.id.buttonJump).setOnClickListener(l);
    d.findViewById(R.id.buttonPunch).setOnClickListener(l);
    d.findViewById(R.id.buttonBomb).setOnClickListener(l);
    d.findViewById(R.id.buttonRun1).setOnClickListener(l);
    d.findViewById(R.id.buttonRun2).setOnClickListener(l);
    d.findViewById(R.id.buttonStart).setOnClickListener(l);

    d.setTitle(R.string.configHardwareButtons);
    _updateHardwareControlsLabels(d);
    d.show();

  }

  private Set<Integer> mHeldKeys = new TreeSet<>();
  private boolean mHeldTriggerL = false;
  private boolean mHeldTriggerR = false;

  private float mPhysicalJoystickAxisValueX = 0.0f;
  private float mPhysicalJoystickAxisValueY = 0.0f;
  private float mPhysicalJoystickDPadValueX = 0.0f;
  private float mPhysicalJoystickDPadValueY = 0.0f;

  // Generic-motion events
  @Override
  public boolean onGenericMotionEvent(MotionEvent event) {

    boolean handled = false;
    boolean changed = false;

    if ((event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {

      final int historySize = event.getHistorySize();
      // Append all historical values in the batch.
      for (int historyPos = 0; historyPos < (historySize + 1); historyPos++) {

        float valueAxisX;
        float valueAxisY;
        float valueDPadX;
        float valueDPadY;
        float valueTriggerL;
        float valueTriggerR;

        // go through historical values and current
        if (historyPos < historySize) {
          valueAxisX =
              event.getHistoricalAxisValue(MotionEvent.AXIS_X, historyPos);
          valueAxisY =
              event.getHistoricalAxisValue(MotionEvent.AXIS_Y, historyPos);
          valueDPadX =
              event.getHistoricalAxisValue(MotionEvent.AXIS_HAT_X, historyPos);
          valueDPadY =
              event.getHistoricalAxisValue(MotionEvent.AXIS_HAT_Y, historyPos);
          valueTriggerL = event
              .getHistoricalAxisValue(MotionEvent.AXIS_LTRIGGER, historyPos);
          valueTriggerR = event
              .getHistoricalAxisValue(MotionEvent.AXIS_RTRIGGER, historyPos);

        } else {
          valueAxisX = event.getAxisValue(MotionEvent.AXIS_X);
          valueAxisY = event.getAxisValue(MotionEvent.AXIS_Y);
          valueDPadX = event.getAxisValue(MotionEvent.AXIS_HAT_X);
          valueDPadY = event.getAxisValue(MotionEvent.AXIS_HAT_Y);
          valueTriggerL = event.getAxisValue(MotionEvent.AXIS_LTRIGGER);
          valueTriggerR = event.getAxisValue(MotionEvent.AXIS_RTRIGGER);

        }
        boolean triggerHeldL = (valueTriggerL >= 0.5);
        boolean triggerHeldR = (valueTriggerR >= 0.5);

        // handle trigger state changes
        if (triggerHeldL != mHeldTriggerL || triggerHeldR != mHeldTriggerR) {
          boolean runWasHeld =
              ((!this.mHeldKeys.isEmpty()) || mHeldTriggerL || mHeldTriggerR);
          mHeldTriggerL = triggerHeldL;
          mHeldTriggerR = triggerHeldR;
          boolean runIsHeld =
              ((!this.mHeldKeys.isEmpty()) || mHeldTriggerL || mHeldTriggerR);
          if (!runWasHeld && runIsHeld) {
            _handleRunPress();
          }
          if (runWasHeld && !runIsHeld) {
            _handleRunRelease();
          }
          changed = true;
        }

        // handle dpad state changes
        if (_dPadTouch == -1 && (valueAxisX != mPhysicalJoystickAxisValueX ||
            valueAxisY != mPhysicalJoystickAxisValueY ||
            valueDPadX != mPhysicalJoystickDPadValueX ||
            valueDPadY != mPhysicalJoystickDPadValueY)) {
          float valueX;
          float valueY;
          boolean valid;
          // if our dpad has changed, use that value..
          if (valueDPadX != mPhysicalJoystickDPadValueX ||
              valueDPadY != mPhysicalJoystickDPadValueY) {
            valueX = valueDPadX;
            valueY = valueDPadY;
            valid = true;
          } else {
            // otherwise, use the normal axis value *unless* we've got a dpad
            // press going on
            // (wanna avoid having analog axis noise wipe out dpad presses)
            valueX = valueAxisX;
            valueY = valueAxisY;
            valid = (Math.abs(mPhysicalJoystickDPadValueX) < 0.1 &&
                Math.abs(mPhysicalJoystickDPadValueY) < 0.1);
          }
          mPhysicalJoystickAxisValueX = valueAxisX;
          mPhysicalJoystickAxisValueY = valueAxisY;
          mPhysicalJoystickDPadValueX = valueDPadX;
          mPhysicalJoystickDPadValueY = valueDPadY;

          if (valid) {
            float s = 30.0f / _dPadScale;
            if (valueX < -1.0f) {
              valueX = -1.0f;
            } else if (valueX > 1.0f) {
              valueX = 1.0f;
            }
            if (valueY < -1.0f) {
              valueY = -1.0f;
            } else if (valueY > 1.0f) {
              valueY = 1.0f;
            }

            _gl.joystickX = _gl.joystickCenterX + valueX / s;
            _gl.joystickY = _gl.joystickCenterY + valueY / s;

            _gamePadActivity._dPadStateH = valueX;
            _gamePadActivity._dPadStateV = valueY;
            _gamePadActivity._doStateChange(false);

            changed = true;
          }
        }
        handled = true;
      }
    }
    if (changed) {
      requestRender();
    }
    return handled;
  }

  private float mPhysicalDPadDownVal = 0.0f;
  private float mPhysicalDPadUpVal = 0.0f;
  private float mPhysicalDPadLeftVal = 0.0f;
  private float mPhysicalDPadRightVal = 0.0f;

  private void handlePhysicalDPadEvent() {
    float valueX = mPhysicalDPadRightVal - mPhysicalDPadLeftVal;
    float valueY = mPhysicalDPadDownVal - mPhysicalDPadUpVal;

    float s = 30.0f / _dPadScale;
    if (valueX < -1.0f) {
      valueX = -1.0f;
    } else if (valueX > 1.0f) {
      valueX = 1.0f;
    }
    if (valueY < -1.0f) {
      valueY = -1.0f;
    } else if (valueY > 1.0f) {
      valueY = 1.0f;
    }

    _gl.joystickX = _gl.joystickCenterX + valueX / s;
    _gl.joystickY = _gl.joystickCenterY + valueY / s;

    _gamePadActivity._dPadStateH = valueX;
    _gamePadActivity._dPadStateV = valueY;
    _gamePadActivity._doStateChange(false);
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    boolean throwWasHeld = ((_gamePadActivity._buttonStateV1 &
        GamePadActivity.BS_REMOTE_STATE_THROW) != 0);
    boolean jumpWasHeld = ((_gamePadActivity._buttonStateV1 &
        GamePadActivity.BS_REMOTE_STATE_JUMP) != 0);
    boolean punchWasHeld = ((_gamePadActivity._buttonStateV1 &
        GamePadActivity.BS_REMOTE_STATE_PUNCH) != 0);
    boolean bombWasHeld = ((_gamePadActivity._buttonStateV1 &
        GamePadActivity.BS_REMOTE_STATE_BOMB) != 0);
    boolean menuWasHeld = ((_gamePadActivity._buttonStateV1 &
        GamePadActivity.BS_REMOTE_STATE_MENU) != 0);
    boolean handled = false;

    // check for custom assigned keys:
    if (keyCode == _keyPickUp) {
      if (!throwWasHeld) {
        _handleThrowPress();
      }
      handled = true;
    } else if (keyCode == _keyJump) {
      if (!jumpWasHeld) {
        _handleJumpPress();
      }
      handled = true;
    } else if (keyCode == _keyPunch) {
      if (!punchWasHeld) {
        _handlePunchPress();
      }
      handled = true;
    } else if (keyCode == _keyBomb) {
      if (!bombWasHeld) {
        _handleBombPress();
      }
      handled = true;
    } else if (keyCode == _keyStart) {
      if (!menuWasHeld) {
        _handleMenuPress();
      }
      handled = true;
    } else if ((keyCode == _keyRun1) || (keyCode == _keyRun2)) {
      boolean runWasHeld =
          ((!this.mHeldKeys.isEmpty()) || mHeldTriggerL || mHeldTriggerR);
      Integer kcInt = keyCode;
      this.mHeldKeys.add(kcInt);
      boolean runIsHeld =
          ((!this.mHeldKeys.isEmpty()) || mHeldTriggerL || mHeldTriggerR);
      if (!runWasHeld && runIsHeld) {
        _handleRunPress();
      }
      handled = true;
    } else {
      // resort to hard-coded defaults..
      switch (keyCode) {

        case KeyEvent.KEYCODE_DPAD_UP:
          mPhysicalDPadUpVal = 1.0f;
          handlePhysicalDPadEvent();
          handled = true;
          break;
        case KeyEvent.KEYCODE_DPAD_DOWN:
          mPhysicalDPadDownVal = 1.0f;
          handlePhysicalDPadEvent();
          handled = true;
          break;
        case KeyEvent.KEYCODE_DPAD_LEFT:
          mPhysicalDPadLeftVal = 1.0f;
          handlePhysicalDPadEvent();
          handled = true;
          break;
        case KeyEvent.KEYCODE_DPAD_RIGHT:
          mPhysicalDPadRightVal = 1.0f;
          handlePhysicalDPadEvent();
          handled = true;
          break;

        case KeyEvent.KEYCODE_BUTTON_A:
          if (!jumpWasHeld) {
            _handleJumpPress();
          }
          handled = true;
          break;
        case KeyEvent.KEYCODE_BUTTON_B:
          if (!bombWasHeld) {
            _handleBombPress();
          }
          handled = true;
          break;
        case KeyEvent.KEYCODE_BUTTON_X:
          if (!punchWasHeld) {
            _handlePunchPress();
          }
          handled = true;
          break;
        case KeyEvent.KEYCODE_BUTTON_Y:
          if (!throwWasHeld) {
            _handleThrowPress();
          }
          handled = true;
          break;
        case KeyEvent.KEYCODE_MENU:
        case KeyEvent.KEYCODE_BUTTON_START:
          if (!menuWasHeld) {
            _handleMenuPress();
          }
          handled = true;
          break;
        default:
          if (_isRunKey(keyCode)) {
            boolean runWasHeld =
                ((!this.mHeldKeys.isEmpty()) || mHeldTriggerL || mHeldTriggerR);
            Integer kcInt = keyCode;
            this.mHeldKeys.add(kcInt);
            boolean runIsHeld =
                ((!this.mHeldKeys.isEmpty()) || mHeldTriggerL || mHeldTriggerR);
            if (!runWasHeld && runIsHeld) {
              _handleRunPress();
            }
            handled = true;
          }
          break;
      }
    }
    if (handled) {
      requestRender();
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  boolean _isRunKey(int keyCode) {
    switch (keyCode) {
      case KeyEvent.KEYCODE_BUTTON_R1:
      case KeyEvent.KEYCODE_BUTTON_R2:
      case KeyEvent.KEYCODE_BUTTON_L1:
      case KeyEvent.KEYCODE_BUTTON_L2:
      case KeyEvent.KEYCODE_VOLUME_UP:
      case KeyEvent.KEYCODE_VOLUME_DOWN:
        return true;
      default:
        return false;
    }
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {

    boolean throwWasHeld = ((_gamePadActivity._buttonStateV1 &
        GamePadActivity.BS_REMOTE_STATE_THROW) != 0);
    boolean jumpWasHeld = ((_gamePadActivity._buttonStateV1 &
        GamePadActivity.BS_REMOTE_STATE_JUMP) != 0);
    boolean punchWasHeld = ((_gamePadActivity._buttonStateV1 &
        GamePadActivity.BS_REMOTE_STATE_PUNCH) != 0);
    boolean bombWasHeld = ((_gamePadActivity._buttonStateV1 &
        GamePadActivity.BS_REMOTE_STATE_BOMB) != 0);
    boolean menuWasHeld = ((_gamePadActivity._buttonStateV1 &
        GamePadActivity.BS_REMOTE_STATE_MENU) != 0);

    boolean handled = false;

    // handle our custom-assigned keys
    if (keyCode == _keyPickUp) {
      if (throwWasHeld) {
        _handleThrowRelease();
      }
      handled = true;
    } else if (keyCode == _keyJump) {
      if (jumpWasHeld) {
        _handleJumpRelease();
      }
      handled = true;
    } else if (keyCode == _keyPunch) {
      if (punchWasHeld) {
        _handlePunchRelease();
      }
      handled = true;
    } else if (keyCode == _keyBomb) {
      if (bombWasHeld) {
        _handleBombRelease();
      }
      handled = true;
    } else if (keyCode == _keyStart) {
      if (menuWasHeld) {
        _handleMenuRelease();
      }
      handled = true;
    } else if ((keyCode == _keyRun1) || (keyCode == _keyRun2)) {
      boolean runWasHeld =
          ((!this.mHeldKeys.isEmpty()) || mHeldTriggerL || mHeldTriggerR);
      Integer kcInt = keyCode;
      if (this.mHeldKeys.contains(kcInt)) {
        this.mHeldKeys.remove(kcInt);
      }
      boolean runIsHeld =
          ((!this.mHeldKeys.isEmpty()) || mHeldTriggerL || mHeldTriggerR);
      if (runWasHeld && !runIsHeld) {
        _handleRunRelease();
      }
      handled = true;
    } else {
      // fall back on hard-coded defaults
      switch (keyCode) {

        case KeyEvent.KEYCODE_DPAD_UP:
          mPhysicalDPadUpVal = 0.0f;
          handlePhysicalDPadEvent();
          handled = true;
          break;
        case KeyEvent.KEYCODE_DPAD_DOWN:
          mPhysicalDPadDownVal = 0.0f;
          handlePhysicalDPadEvent();
          handled = true;
          break;
        case KeyEvent.KEYCODE_DPAD_LEFT:
          mPhysicalDPadLeftVal = 0.0f;
          handlePhysicalDPadEvent();
          handled = true;
          break;
        case KeyEvent.KEYCODE_DPAD_RIGHT:
          mPhysicalDPadRightVal = 0.0f;
          handlePhysicalDPadEvent();
          handled = true;
          break;

        case KeyEvent.KEYCODE_BUTTON_A:
          if (jumpWasHeld) {
            _handleJumpRelease();
          }
          handled = true;
          break;
        case KeyEvent.KEYCODE_BUTTON_B:
          if (bombWasHeld) {
            _handleBombRelease();
          }
          handled = true;
          break;
        case KeyEvent.KEYCODE_BUTTON_X:
          if (punchWasHeld) {
            _handlePunchRelease();
          }
          handled = true;
          break;
        case KeyEvent.KEYCODE_BUTTON_Y:
          if (throwWasHeld) {
            _handleThrowRelease();
          }
          handled = true;
          break;
        case KeyEvent.KEYCODE_MENU:
        case KeyEvent.KEYCODE_BUTTON_START:
          if (menuWasHeld) {
            _handleMenuRelease();
          }
          handled = true;
          break;
        default:
          if (_isRunKey(keyCode)) {
            boolean runWasHeld =
                ((!this.mHeldKeys.isEmpty()) || mHeldTriggerL || mHeldTriggerR);
            Integer kcInt = keyCode;
            this.mHeldKeys.remove(kcInt);
            boolean runIsHeld =
                ((!this.mHeldKeys.isEmpty()) || mHeldTriggerL || mHeldTriggerR);
            if (runWasHeld && !runIsHeld) {
              _handleRunRelease();
            }
            handled = true;
          }
          break;
      }
    }

    if (handled) {
      requestRender();
      return true;
    }
    return super.onKeyUp(keyCode, event);
  }

  // Touch events
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    final int pointerCount = event.getPointerCount();
    int actionPointerIndex = event.getActionIndex();
    int action = event.getActionMasked();

    float mult = 1.0f / getWidth();

    // touch margin
    float tm = 1.4f;
    float tm2 = 2.4f;

    for (int i = 0; i < pointerCount; i++) {
      float x = event.getX(i) * mult;
      float y = event.getY(i) * mult;
      int fingerID = event.getPointerId(i);

      if ((action == MotionEvent.ACTION_DOWN ||
          action == MotionEvent.ACTION_POINTER_DOWN) &&
          (actionPointerIndex == i)) {

        // prefs presses
        if (_pointInBox(x, y, _gl.prefsButtonX, _gl.prefsButtonY,
            _gl.prefsButtonWidth * tm, _gl.prefsButtonHeight * tm2)) {
          _prefsTouch = fingerID;
          _prefsHover = true;
          _gl.prefsButtonPressed = true;
        }

        // check for a quit press
        else if (_pointInBox(x, y, _gl.quitButtonX, _gl.quitButtonY,
            _gl.quitButtonWidth * tm, _gl.quitButtonHeight * tm2)) {
          _quitTouch = fingerID;
          _quitHover = true;
          _gl.quitButtonPressed = true;
        }
        // check for a start press
        else if (_pointInBox(x, y, _gl.startButtonX, _gl.startButtonY,
            _gl.startButtonWidth * tm, _gl.startButtonHeight * tm2)) {
          _startTouch = fingerID;
          _startHover = true;
          _gl.startButtonPressed = true;
        }
        // check for a dpad touch
        else if (x < 0.5) {
          _dPadTouch = fingerID;
          _dPadTouchIsMove = false;
          // in fixed joystick mode we want touches to count towards
          // joystick motion immediately; not just after they move
          if (_dPadType.equals("fixed")) {
            _dPadTouchIsMove = true;
          }

          _dPadTouchStartX = x;
          _dPadTouchStartY = y;
          _gl.joystickX = x;
          _gl.joystickY = y;
          _gl.thumbPressed = true;

        }
      }
      // handle existing button touches
      if (fingerID == _quitTouch) {
        // update position
        _quitHover = _gl.quitButtonPressed =
            (_pointInBox(x, y, _gl.quitButtonX, _gl.quitButtonY,
                _gl.quitButtonWidth * tm, _gl.quitButtonHeight * tm2));
        // if the touch is ending...
        if ((action == MotionEvent.ACTION_UP ||
            action == MotionEvent.ACTION_POINTER_UP) &&
            (actionPointerIndex == i)) {
          _quitTouch = -1;
          //_quitHover = false;
          _gl.quitButtonPressed = false;
          if (_quitHover) {

            // ewwww - seeing that in some cases our onDestroy()
            // doesn't get called for a while which keeps the server
            // from announcing our departure.  lets just shoot off
            // one random disconnect packet here to hopefully speed that along.
            _gamePadActivity.sendDisconnectPacket();
            _gamePadActivity.finish();
          }
        }
      } else if (fingerID == _prefsTouch) {
        // update position
        _prefsHover = _gl.prefsButtonPressed =
            (_pointInBox(x, y, _gl.prefsButtonX, _gl.prefsButtonY,
                _gl.prefsButtonWidth * tm, _gl.prefsButtonHeight * tm2));
        // if the touch is ending...
        if ((action == MotionEvent.ACTION_UP ||
            action == MotionEvent.ACTION_POINTER_UP) &&
            (actionPointerIndex == i)) {
          _prefsTouch = -1;
          _gl.prefsButtonPressed = false;
          if (_prefsHover) {
            doPrefs();
          }
        }
      } else if (fingerID == _startTouch) {
        // update position
        _startHover = _gl.startButtonPressed =
            (_pointInBox(x, y, _gl.startButtonX, _gl.startButtonY,
                _gl.startButtonWidth * tm, _gl.startButtonHeight * tm2));
        // if the touch is ending...
        if ((action == MotionEvent.ACTION_UP ||
            action == MotionEvent.ACTION_POINTER_UP) &&
            (actionPointerIndex == i)) {
          _startTouch = -1;
          _gl.startButtonPressed = false;
          if (_startHover) {
            // send 2 state-changes (start-down and start-up)
            _gamePadActivity._buttonStateV1 |=
                GamePadActivity.BS_REMOTE_STATE_MENU;
            _gamePadActivity._buttonStateV2 |=
                GamePadActivity.BS_REMOTE_STATE2_MENU;
            _gamePadActivity._doStateChange(false);
            _gamePadActivity._buttonStateV1 &=
                ~GamePadActivity.BS_REMOTE_STATE_MENU;
            _gamePadActivity._buttonStateV2 &=
                ~GamePadActivity.BS_REMOTE_STATE2_MENU;
            _gamePadActivity._doStateChange(false);
          }
        }
      }


      // if its our existing dpad-touch
      if (fingerID == _dPadTouch) {

        // if we've moved away from the initial touch position we no longer
        // consider it for a direction tap
        if (!_dPadTouchIsMove && (Math.abs(x - _dPadTouchStartX) > 0.01 ||
            Math.abs(y - _dPadTouchStartY) > 0.01)) {
          _dPadTouchIsMove = true;
          _gl.joystickCenterX = _dPadTouchStartX;
          _gl.joystickCenterY = _dPadTouchStartY;
        }
        // if its moved, pass it along as a joystick event
        if (_dPadTouchIsMove) {
          float s = 30.0f / _dPadScale;
          float xVal = (x - _gl.joystickCenterX) * s;
          float yVal = (y - _gl.joystickCenterY) * s;
          float xValClamped = xVal;
          float yValClamped = yVal;

          // keep our H/V values within a unit box
          // (originally I clamped length to 1 but as a result our diagonal
          // running speed was less than other analog controllers..)

          if (xValClamped > 1.0f) {
            float m = 1.0f / xValClamped;
            xValClamped *= m;
            yValClamped *= m;
          } else if (xValClamped < -1.0f) {
            float m = -1.0f / xValClamped;
            xValClamped *= m;
            yValClamped *= m;
          }
          if (yValClamped > 1.0f) {
            float m = 1.0f / yValClamped;
            xValClamped *= m;
            yValClamped *= m;
          } else if (yValClamped < -1.0f) {
            float m = -1.0f / yValClamped;
            xValClamped *= m;
            yValClamped *= m;
          }

          _gl.joystickX = _gl.joystickCenterX + xVal / s;
          _gl.joystickY = _gl.joystickCenterY + yVal / s;

          // if its moved far enough away from center, have the dpad
          // follow it (in floating mode)
          // in fixed mode just clamp distance
          float dist = (float) Math.sqrt(xVal * xVal + yVal * yVal);
          if (_dPadType.equals("floating")) {
            if (dist > 1.5f) {
              float sc = 1.5f / dist;
              _gl.joystickCenterX = _gl.joystickX - sc * (xVal / s);
              _gl.joystickCenterY = _gl.joystickY - sc * (yVal / s);
            }
          } else if (_dPadType.equals("fixed")) {
            if (dist > 1.01f) {
              float sc = 1.01f / dist;
              _gl.joystickX = _gl.joystickCenterX + sc * (xVal / s);
              _gl.joystickY = _gl.joystickCenterY + sc * (yVal / s);
            }

          }

          _gamePadActivity._dPadStateH = xValClamped;
          _gamePadActivity._dPadStateV = yValClamped;
          _gamePadActivity._doStateChange(false);
        }

        // if the touch is ending...
        if ((action == MotionEvent.ACTION_UP ||
            action == MotionEvent.ACTION_POINTER_UP) &&
            (actionPointerIndex == i)) {

          // if we hadnt moved yet, lets pass it along as a quick tap/release
          // (useful for navigating menus)
          if (!_dPadTouchIsMove) {
            float toRight = x - _gl.joystickCenterX;
            float toLeft = _gl.joystickCenterX - x;
            float toBottom = y - _gl.joystickCenterY;
            float toTop = _gl.joystickCenterY - y;
            // right
            if (toRight > toLeft && toRight > toTop && toRight > toBottom) {
              _gamePadActivity._dPadStateH = 1.0f;
              _gamePadActivity._dPadStateV = 0.0f;
              _gamePadActivity._doStateChange(false);
              _gamePadActivity._dPadStateH = 0.0f;
              _gamePadActivity._dPadStateV = 0.0f;
              _gamePadActivity._doStateChange(false);
            }
            // left
            else if (toLeft > toRight && toLeft > toTop && toLeft > toBottom) {
              _gamePadActivity._dPadStateH = -1.0f;
              _gamePadActivity._dPadStateV = 0.0f;
              _gamePadActivity._doStateChange(false);
              _gamePadActivity._dPadStateH = 0.0f;
              _gamePadActivity._dPadStateV = 0.0f;
              _gamePadActivity._doStateChange(false);
            } else if (toTop > toRight && toTop > toLeft && toTop > toBottom) {
              _gamePadActivity._dPadStateH = 0.0f;
              _gamePadActivity._dPadStateV = -1.0f;
              _gamePadActivity._doStateChange(false);
              _gamePadActivity._dPadStateH = 0.0f;
              _gamePadActivity._dPadStateV = 0.0f;
              _gamePadActivity._doStateChange(false);

            } else {
              _gamePadActivity._dPadStateH = 0.0f;
              _gamePadActivity._dPadStateV = 1.0f;
              _gamePadActivity._doStateChange(false);
              _gamePadActivity._dPadStateH = 0.0f;
              _gamePadActivity._dPadStateV = 0.0f;
              _gamePadActivity._doStateChange(false);
            }
          }
          _dPadTouch = -1;
          _gl.thumbPressed = false;
          _gl.joystickCenterX = _dPadCenterX;
          _gl.joystickCenterY = _dPadCenterY;
          _gl.joystickX = _gl.joystickCenterX;
          _gl.joystickY = _gl.joystickCenterY;
          _gamePadActivity._dPadStateH = 0;
          _gamePadActivity._dPadStateV = 0;
          _gamePadActivity._doStateChange(false);
        }
      }
    }
    _updateButtonsForTouches(event);
    requestRender();
    return true;
  }
}

public class GamePadActivity extends Activity {

  public final static boolean debug = false;
  public final static String TAG = "GPA";
  private WorkerThread _readThread;
  private WorkerThread _processThread;
  private Timer _processTimer;
  private Timer _processUITimer;
  private Timer _shutDownTimer;
  private DatagramSocket _socket;
  private InetAddress _addr; // actual address we're talking to
  private InetAddress[] _addrs; // for initial scanning
  private boolean[] _addrsValid;
  private int _port;
  private int _requestID;
  private byte _id;
  long _shutDownStartTime;
  private boolean _dead = false;
  private long _lastLagUpdateTime;
  float _averageLag;
  TextView _lagMeter;
  public boolean mWindowIsFocused = false;
  int _uniqueID;

  static final int BS_REMOTE_ERROR_VERSION_MISMATCH = 0;
  static final int BS_REMOTE_ERROR_GAME_SHUTTING_DOWN = 1;
  static final int BS_REMOTE_ERROR_NOT_ACCEPTING_CONNECTIONS = 2;

  static final int REMOTE_MSG_ID_REQUEST = 2;
  static final int REMOTE_MSG_ID_RESPONSE = 3;
  static final int REMOTE_MSG_DISCONNECT = 4;
  static final int REMOTE_MSG_STATE = 5;
  static final int REMOTE_MSG_STATE_ACK = 6;
  static final int REMOTE_MSG_DISCONNECT_ACK = 7;

  static final int REMOTE_MSG_STATE2 = 10;

  static final int BS_REMOTE_STATE_PUNCH = 1;
  static final int BS_REMOTE_STATE_JUMP = 1 << 1;
  static final int BS_REMOTE_STATE_THROW = 1 << 2;
  static final int BS_REMOTE_STATE_BOMB = 1 << 3;
  static final int BS_REMOTE_STATE_MENU = 1 << 4;
  // (bits 6-10 are d-pad h-value and bits 11-15 are dpad v-value)

  static final int BS_REMOTE_STATE2_MENU = 1;
  static final int BS_REMOTE_STATE2_JUMP = 1 << 1;
  static final int BS_REMOTE_STATE2_PUNCH = 1 << 2;
  static final int BS_REMOTE_STATE2_THROW = 1 << 3;
  static final int BS_REMOTE_STATE2_BOMB = 1 << 4;
  static final int BS_REMOTE_STATE2_RUN = 1 << 5;

  private int _nextState;
  private boolean _connected = false;
  private boolean _shouldPrintConnected;
  private int _requestedState = 0;
  private boolean _shuttingDown = false;
  private long _lastNullStateTime = 0;
  private long[] _stateBirthTimes;
  private long[] _stateLastSentTimes;
  short _buttonStateV1 = 0;
  short _buttonStateV2 = 0;
  float _dPadStateH = 0.0f;
  float _dPadStateV = 0.0f;
  private long _lastSentState = 0;
  short[] _statesV1;
  int[] _statesV2;
  float _currentLag;
  private boolean _usingProtocolV2 = false;
  private MyGLSurfaceView mGLView;
  private String[] _addrsRaw;

  private boolean _newStyle;

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    // keep android from crashing due to our network use in the main thread
    StrictMode.ThreadPolicy policy =
        new StrictMode.ThreadPolicy.Builder().permitAll().build();
    StrictMode.setThreadPolicy(policy);

    // keep the device awake while this activity is visible.
    // granted most of the time the user is tapping on it, but if they have
    // a hardware gamepad attached they might not be.
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    // clients use a random request ID to differentiate themselves
    // (probably is a better way to do this..)
    long currentTime = SystemClock.uptimeMillis();
    _requestID = (int) currentTime % 10000;
    _id = -1; // ain't got one yet
    _stateBirthTimes = new long[256];
    _stateLastSentTimes = new long[256];
    _statesV1 = new short[256];
    _statesV2 = new int[256];

    // if we reconnect we may get acks for states we didnt send..
    // so lets set everything to current time to avoid screwing up
    // our lag-meter
    long curTime = SystemClock.uptimeMillis();
    for (int i = 0; i < 256; i++) {
      _stateBirthTimes[i] = curTime;
      _stateLastSentTimes[i] = 0;
    }

    Bundle extras = getIntent().getExtras();
    if (extras != null) {
      _newStyle = extras.getBoolean("newStyle");
      _port = extras.getInt("connectPort");
      try {
        _socket = new DatagramSocket();
      } catch (SocketException e) {
        LogThread.log("Error setting up gamepad socket", e);
      }
      _addrsRaw = extras.getStringArray("connectAddrs");
    }

    // read or create our random unique ID; we tack this onto our
    // android device identifier just in case its not actually unique
    // (as apparently was the case with nexus-2 or some other device)

    SharedPreferences preferences =
        getSharedPreferences("BSRemotePrefs", Context.MODE_PRIVATE);
    _uniqueID = preferences.getInt("uniqueId", 0);
    if (_uniqueID == 0) {
      while (_uniqueID == 0) {
        _uniqueID = new Random().nextInt() & 0xFFFF;
      }
      SharedPreferences.Editor editor = preferences.edit();
      editor.putInt("uniqueId", _uniqueID);
      editor.apply();
    }

    _processThread = new WorkerThread();
    _processThread.start();

    _readThread = new WorkerThread();
    _readThread.start();

    // all the read-thread does is wait for data to come in
    // and pass it to the process-thread
    _readThread.doRunnable(new Runnable() {
      public void run() {
        while (true) {
          try {
            byte[] buf = new byte[10];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            _socket.receive(packet);
            abstract class PacketRunnable implements Runnable {
              DatagramPacket p;

              PacketRunnable(DatagramPacket pIn) {
                p = pIn;
              }

              public abstract void run();
            }
            _processThread.doRunnable(new PacketRunnable(packet) {
              public void run() {
                GamePadActivity.this._readFromSocket(p);
              }
            });

          } catch (IOException e) {
            // assuming this means the socket is closed..
            if (debug) {
              Log.v(TAG, "READ THREAD DYING");
            }
            _readThread.getLooper().quit();
            _readThread = null;
            break;
          } catch (ArrayIndexOutOfBoundsException e) {
            LogThread.log("Got excessively sized datagram packet", e);
          }
        }
      }
    });

    super.onCreate(savedInstanceState);

    // Create a GLSurfaceView instance and set it
    // as the ContentView for this Activity
    mGLView = new MyGLSurfaceView(this);

    ViewGroup mLayout = new RelativeLayout(this);
    mLayout.addView(mGLView);

    _lagMeter = new TextView(this);
    _lagMeter.setTextColor(0xFF00FF00);
    _lagMeter.setText("--");
    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.WRAP_CONTENT,
        RelativeLayout.LayoutParams.WRAP_CONTENT);
    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
    params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

    params.bottomMargin = 20;
    mLayout.addView(_lagMeter, params);

    setContentView(mLayout);
  }

  public void shutDown() {

    _shuttingDown = true;
    _shutDownStartTime = SystemClock.uptimeMillis();

    // create our shutdown timer.. this will keep us
    // trying to disconnect cleanly with the server until
    // we get confirmation or we give up
    // tell our worker thread to start its update timer
    _processThread.doRunnable(new Runnable() {
      public void run() {
        if (debug) {
          Log.v(TAG, "CREATING SHUTDOWN TIMER...");
        }
        assert (_shutDownTimer == null);
        _shutDownTimer = new Timer();
        _shutDownTimer.schedule(new TimerTask() {
          @Override
          public void run() {
            // when this timer fires, tell our process
            // thread to run
            _processThread.doRunnable(new Runnable() {
              public void run() {
                GamePadActivity.this._process();
              }
            });
          }
        }, 0, 100);
      }
    });

    // let our gl view clean up anything it needs to
    if (mGLView != null) {
      mGLView.onClosing();
    }

  }

  @Override
  public void onDestroy() {

    super.onDestroy();
    if (debug) {
      Log.v(TAG, "onDestroy()");
    }
    shutDown();
  }

  @Override
  protected void onStart() {

    super.onStart();
    if (debug) {
      Log.v(TAG, "GPA onStart()");
    }

    // tell our worker thread to start its update timer
    _processThread.doRunnable(new Runnable() {
      public void run() {
        // kick off an id request... (could just wait for the process
        // timer to do this)
        if (_id == -1) {
          GamePadActivity.this._sendIdRequest();
        }

        if (debug) {
          Log.v(TAG, "CREATING PROCESS TIMER..");
        }
        _processTimer = new Timer();
        _processTimer.schedule(new TimerTask() {
          @Override
          public void run() {
            if (_processThread != null) {
              // when this timer fires, tell our process thread to run
              _processThread.doRunnable(new Runnable() {
                public void run() {
                  GamePadActivity.this._process();
                }
              });
            }
          }
        }, 0, 100);

        // lets also do some upkeep stuff at a slower pace..
        _processUITimer = new Timer();
        _processUITimer.schedule(new TimerTask() {
          @Override
          public void run() {
            // when this timer fires, tell our process
            // thread to run
            runOnUiThread(new Runnable() {
              public void run() {
                GamePadActivity.this._processUI();
              }
            });
          }
        }, 0, 2500);

      }
    });

  }

  protected void onStop() {
    super.onStop();

    _processThread.doRunnable(new Runnable() {
      public void run() {
        _processTimer.cancel();
        _processTimer.purge();
        _processUITimer.cancel();
        _processUITimer.purge();
      }
    });


  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    return super.onKeyDown(keyCode, event);
  }

  // handle periodic processing such as receipt re-requests
  protected void _processUI() {
    // eww as of android 4.4.2 there's several things that cause
    // immersive mode state to get lost.. (such as volume up/down buttons)
    // ...so for now lets force the issue
    if (android.os.Build.VERSION.SDK_INT >= 19 && mWindowIsFocused) {
      _setImmersiveMode();
    }
  }

  @TargetApi(19)
  private void _setImmersiveMode() {
    int vis = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN |
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    if (mGLView.getSystemUiVisibility() != vis) {
      mGLView.setSystemUiVisibility(vis);
    }
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    mWindowIsFocused = hasFocus;
    if (android.os.Build.VERSION.SDK_INT >= 19 && hasFocus) {
      _setImmersiveMode();
    }
  }

  public void sendDisconnectPacket() {
    if (_id != -1) {
      byte[] data = new byte[2];
      data[0] = REMOTE_MSG_DISCONNECT;
      data[1] = _id;
      try {
        _socket.send(new DatagramPacket(data, data.length, _addr, _port));
      } catch (IOException e) {
        LogThread.log("", e);
      }
    }

  }

  private void _process() {
    if (BuildConfig.DEBUG && Thread.currentThread() != _processThread) {
      throw new AssertionError("thread error");
    }

    // if any of these happen after we're dead totally ignore them
    if (_dead) {
      return;
    }


    long t = SystemClock.uptimeMillis();

    // if we're shutting down but are still connected, keep sending
    // disconnects out
    if (_shuttingDown) {

      boolean finishUsOff = false;

      // shoot off another disconnect notice
      // once we're officially disconnected we can die
      if (!_connected) {
        finishUsOff = true;
      } else {
        sendDisconnectPacket();
        // just give up after a short while
        if (t - _shutDownStartTime > 5000) {
          finishUsOff = true;
        }
      }
      if (finishUsOff && !_dead) {
        _dead = true;
        // ok we're officially dead.. clear out our threads/timers/etc
        _shutDownTimer.cancel();
        _shutDownTimer.purge();
        _processThread.getLooper().quit();
        _processThread = null;
        // this should kill our read-thread
        _socket.close();
        return;
      }
    }

    // if we've got states we havn't heard an ack for yet, keep shipping 'em
    // out
    int stateDiff = (_requestedState - _nextState) & 0xFF;
    if (BuildConfig.DEBUG && stateDiff < 0) {
      throw new AssertionError();
    }

    // if they've requested a state we don't have yet, we don't need to
    // resend anything
    if (stateDiff < 128) {
      // .. however we wanna shoot states at the server every now and then
      // even if we have no new states,
      // to keep from timing out and such..
      if (t - _lastNullStateTime > 3000) {
        _doStateChange(true);
        _lastNullStateTime = t;
      }
    } else {
      // ok we've got at least one state we havn't heard confirmation for
      // yet.. lets ship 'em out..
      if (_usingProtocolV2) {
        _shipUnAckedStatesV2();
      } else {
        _shipUnAckedStatesV1();
      }
    }

    // if we don't have an ID yet, keep sending off those requests...
    if (_id == -1) {
      _sendIdRequest();
    }

    // update our lag meter every so often
    if (t - _lastLagUpdateTime > 2000) {
      float smoothing = 0.5f;
      _averageLag = smoothing * _averageLag + (1.0f - smoothing) * _currentLag;

      // lets show half of our the round-trip time as lag.. (the actual
      // delay
      // in-game is just our packets getting to them; not the round trip)
      runOnUiThread(new Runnable() {
        @SuppressLint("StringFormatInvalid")
        public void run() {
          if (_shouldPrintConnected) {
            _lagMeter.setTextColor(0xFF00FF00);
            _lagMeter.setText(R.string.connected);
            _shouldPrintConnected = false;

          } else if (_connected) {
            // convert from millisecs to seconds
            float val = (_averageLag * 0.5f) / 1000.0f;
            if (val < 0.1) {
              _lagMeter.setTextColor(0xFF88FF88);
            } else if (val < 0.2) {
              _lagMeter.setTextColor(0xFFFFB366);
            } else {
              _lagMeter.setTextColor(0xFFFF6666);
            }
            _lagMeter.setText(String
                .format(getString(R.string.lag).replace("${SECONDS}", "%.2f"),
                    val));
          } else {
            // connecting...
            _lagMeter.setTextColor(0xFFFF8800);
            _lagMeter.setText(R.string.connecting);
          }
        }
      });
      _currentLag = 0.0f;
      _lastLagUpdateTime = t;
    }
  }

  private void _shipUnAckedStatesV1() {

    // if we don't have an id yet this is moot..
    if (_id == -1) {
      return;
    }

    long curTime = SystemClock.uptimeMillis();

    // ok we need to ship out everything from their last requested state
    // to our current state.. (clamping at a reasonable value)
    if (_id != -1) {

      int statesToSend = (_nextState - _requestedState) & 0xFF;
      if (statesToSend > 11) {
        statesToSend = 11;
      }
      if (statesToSend < 1) {
        return;
      }

      byte[] data = new byte[100];
      data[0] = REMOTE_MSG_STATE;
      data[1] = _id;
      data[2] = (byte) statesToSend; // number of states we have here

      int s = (_nextState - statesToSend) & 0xFF;
      if (debug) {
        Log.v(TAG, "SENDING " + statesToSend + " STATES FROM: " + s);
      }
      data[3] = (byte) (s & 0xFF); // starting index

      // pack em in
      int index = 4;
      for (int i = 0; i < statesToSend; i++) {
        data[index++] = (byte) (_statesV1[s] & 0xFF);
        data[index++] = (byte) (_statesV1[s] >> 8);
        _stateLastSentTimes[s] = curTime;
        s = (s + 1) % 256;
      }
      if (_connected) {
        try {
          _socket.send(
              new DatagramPacket(data, 4 + 2 * statesToSend, _addr, _port));
        } catch (IOException e) {

          // if anything went wrong here, assume the game just shut down
          runOnUiThread(new Runnable() {
            public void run() {
              String msg = getString(R.string.gameShutDown);
              Context context = getApplicationContext();
              int duration = Toast.LENGTH_LONG;
              Toast toast = Toast.makeText(context, msg, duration);
              toast.show();
            }

          });
          _connected = false;
          finish();

        }
      }
    }
  }

  private void _shipUnAckedStatesV2() {

    // if we don't have an id yet this is moot..
    if (_id == -1) {
      return;
    }

    long curTime = SystemClock.uptimeMillis();

    // ok we need to ship out everything from their last requested state
    // to our current state.. (clamping at a reasonable value)
    if (_id != -1) {

      int statesToSend = (_nextState - _requestedState) & 0xFF;
      if (statesToSend > 11) {
        statesToSend = 11;
      }
      if (statesToSend < 1) {
        return;
      }

      byte[] data = new byte[150];
      data[0] = REMOTE_MSG_STATE2;
      data[1] = _id;
      data[2] = (byte) statesToSend; // number of states we have here

      int s = (_nextState - statesToSend) & 0xFF;
      if (debug) {
        Log.v(TAG, "SENDING " + statesToSend + " STATES FROM: " + s);
      }
      data[3] = (byte) (s & 0xFF); // starting index

      // pack em in
      int index = 4;
      for (int i = 0; i < statesToSend; i++) {
        data[index++] = (byte) (_statesV2[s] & 0xFF);
        data[index++] = (byte) (_statesV2[s] >> 8);
        data[index++] = (byte) (_statesV2[s] >> 16);
        _stateLastSentTimes[s] = curTime;
        s = (s + 1) % 256;
      }
      if (_connected) {
        try {
          _socket.send(
              new DatagramPacket(data, 4 + 3 * statesToSend, _addr, _port));
        } catch (IOException e) {

          // if anything went wrong here, assume the game just shut down
          runOnUiThread(new Runnable() {
            public void run() {
              String msg = getString(R.string.gameShutDown);
              Context context = getApplicationContext();
              int duration = Toast.LENGTH_LONG;
              Toast toast = Toast.makeText(context, msg, duration);
              toast.show();
            }

          });
          _connected = false;
          finish();

        }
      }
    }
  }

  void _doStateChange(boolean force) {
    if (_usingProtocolV2) {
      _doStateChangeV2(force);
    } else {
      _doStateChangeV1(force);
    }
  }

  void _doStateChangeV2(boolean force) {

    // compile our state value
    int s = _buttonStateV2; // buttons
    int hVal = (int) (256.0f * (0.5f + _dPadStateH * 0.5f));
    if (hVal < 0) {
      hVal = 0;
    } else if (hVal > 255) {
      hVal = 255;
    }

    int vVal = (int) (256.0f * (0.5f + _dPadStateV * 0.5f));
    if (vVal < 0) {
      vVal = 0;
    } else if (vVal > 255) {
      vVal = 255;
    }

    s |= hVal << 8;
    s |= vVal << 16;

    // if our compiled state value hasn't changed, don't send.
    // (analog joystick noise can send a bunch of redundant states through
    // here)
    // The exception is if forced is true, which is the case with packets
    // that double as keepalives.
    if ((s == _lastSentState) && (!force)) {
      return;
    }

    _stateBirthTimes[_nextState] = SystemClock.uptimeMillis();
    _stateLastSentTimes[_nextState] = 0;

    if (debug) {
      Log.v(TAG, "STORING NEXT STATE: " + _nextState);
    }
    _statesV2[_nextState] = s;
    _nextState = (_nextState + 1) % 256;
    _lastSentState = s;

    // if we're pretty up to date as far as state acks, lets go ahead
    // and send out this state immediately..
    // (keeps us nice and responsive on low latency networks)
    int unackedCount = (_nextState - _requestedState) & 0xFF; // upcast to
    // get unsigned
    if (unackedCount < 3) {
      _shipUnAckedStatesV2();
    }
  }

  void _doStateChangeV1(boolean force) {

    // compile our state value
    short s = _buttonStateV1; // buttons
    s |= (_dPadStateH > 0 ? 1 : 0) << 5; // sign bit
    s |= ((int) (Math.round(Math.min(1.0, Math.abs(_dPadStateH)) * 15.0))) <<
        6; // mag
    s |= (_dPadStateV > 0 ? 1 : 0) << 10; // sign bit
    s |= ((int) (Math.round(Math.min(1.0, Math.abs(_dPadStateV)) * 15.0))) <<
        11; // mag

    // if our compiled state value hasn't changed, don't send.
    // (analog joystick noise can send a bunch of redundant states through here)
    // The exception is if forced is true, which is the case with packets that
    // double as keepalives.
    if ((s == _lastSentState) && (!force)) {
      return;
    }

    _stateBirthTimes[_nextState] = SystemClock.uptimeMillis();
    _stateLastSentTimes[_nextState] = 0;

    if (debug) {
      Log.v(TAG, "STORING NEXT STATE: " + _nextState);
    }
    _statesV1[_nextState] = s;
    _nextState = (_nextState + 1) % 256;
    _lastSentState = s;

    // if we're pretty up to date as far as state acks, lets go ahead
    // and send out this state immediately..
    // (keeps us nice and responsive on low latency networks)
    int unackedCount = (_nextState - _requestedState) & 0xFF; // upcast to
    // get
    // unsigned
    if (unackedCount < 3) {
      _shipUnAckedStatesV1();
    }
  }

  private void _sendIdRequest() {
    if (_connected) {
      throw new AssertionError();
    }
    if (_id != -1) {
      throw new AssertionError();
    }
    if (Thread.currentThread() != _processThread) {
      throw new AssertionError();
    }

    // get our unique identifier to tack onto the end of our name
    // (so if we drop our connection and reconnect we can be reunited with
    // our old
    // dude instead of leaving a zombie)
    String android_id = Secure
        .getString(getApplicationContext().getContentResolver(),
            Secure.ANDROID_ID);

    // on new-style connections we include unique id info in our name so we
    // can be re-connected
    // if we disconnect
    String deviceName;
    if (_newStyle) {
      String name = ScanActivity.getPlayerName();
      // make sure we don't have any #s in it
      name = name.replaceAll("#", "");
      deviceName = name + "#" + android_id + _uniqueID;

    } else {
      deviceName = "Android remote (" + android.os.Build.MODEL + ")";
    }
    byte[] nameBytes = new byte[0];
    try {
      nameBytes = deviceName.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      LogThread.log("Error getting bytes from name", e);
    }
    int dLen = nameBytes.length;
    // int dLen = deviceName.length();

    if (dLen > 99) {
      dLen = 99;
    }

    // send a hello on all our addrs
    //Log.v(TAG, "Sending ID Request");
    byte[] data = new byte[128];
    data[0] = REMOTE_MSG_ID_REQUEST;
    data[1] = 121; // old protocol version..
    data[2] = (byte) (_requestID & 0xFF);
    data[3] = (byte) (_requestID >> 8);
    data[4] = 50; // protocol version request (this implies we support
    // state-packet-2 if they do)
    int len = 5;
    for (int i = 0; i < dLen; i++) {
      data[5 + i] = nameBytes[i];
      len++;
    }
    // resolve our addresses if we haven't yet (couldn't do this in main thread)
    if (_addrs == null) {
      int i = 0;
      _addrs = new InetAddress[_addrsRaw.length];
      _addrsValid = new boolean[_addrsRaw.length];
      for (String a : _addrsRaw) {
        try {
          _addrsValid[i] = false;
          _addrs[i] = InetAddress.getByName(a);
          _addrsValid[i] = true;
        } catch (UnknownHostException e) {
          runOnUiThread(new Runnable() {
            public void run() {
              String msg = getString(R.string.cantResolveHost);
              Context context = getApplicationContext();
              int duration = Toast.LENGTH_LONG;
              Toast toast = Toast.makeText(context, msg, duration);
              toast.show();
              _lagMeter.setText(msg);
              _lagMeter.setVisibility(View.INVISIBLE);
            }

          });
        }
        i++;
      }
    }
    {
      boolean haveValidAddress = false;
      for (int i = 0; i < _addrs.length; i++) {
        if (!_addrsValid[i]) {
          continue;
        }
        haveValidAddress = true;
        InetAddress addr = _addrs[i];

        DatagramPacket packet = new DatagramPacket(data, len, addr, _port);
        try {
          _socket.send(packet);
        } catch (IOException e) {
          LogThread.log("Error on ID-request send", e);
          Log.e(TAG, "Error on ID-request send: " + e.getMessage());
          e.printStackTrace();
        } catch (NullPointerException e) {
          LogThread.log("Error on ID-request send", e);
          Log.e(TAG, "Bad IP specified: " + e.getMessage());
        }
        i++;
      }
      // if no addresses were valid, lets just quit out..
      if (!haveValidAddress) {
        finish();
      }
    }
  }

  private void _readFromSocket(DatagramPacket packet) {
    byte[] buffer = packet.getData();
    int amt = packet.getLength();
    if (Thread.currentThread() != _processThread) {
      throw new AssertionError();
    }
    if (amt > 0) {
      if (buffer[0] == REMOTE_MSG_ID_RESPONSE) {
        if (debug) {
          Log.v(TAG, "Got ID response");
        }
        if (amt == 3) {
          if (_connected) {
            if (debug) {
              Log.v(TAG, "Already connected; ignoring ID response");
            }
            return;
          }
          // for whatever reason .connect started behaving wonky in android 7
          // or so
          // ..(perhaps ipv6 related?..)
          // ..looks like just grabbing the addr and feeding it to our outgoing
          // datagrams works though
          _addr = packet.getAddress();
          // hooray we have an id.. we're now officially connected
          _id = buffer[1];

          // we said we support protocol v2.. if they respond with 100, they
          // do too.
          _usingProtocolV2 = (buffer[2] == 100);
          _nextState = 0; // start over with this ID
          _connected = true;
          _shouldPrintConnected = true;
          if (_id == -1) {
            throw new AssertionError();
          }
        } else {
          Log.e(TAG, "INVALID ID RESPONSE!");
        }
      } else if (buffer[0] == REMOTE_MSG_STATE_ACK) {
        if (amt == 2) {
          long time = SystemClock.uptimeMillis();
          // take note of the next state they want...
          // move ours up to that point (if we haven't yet)
          if (debug) {
            Log.v(TAG, "GOT STATE ACK TO " + (buffer[1] & 0xFF));
          }
          int stateDiff = (buffer[1] - _requestedState) & 0xFF; // upcast to
          // positive
          if (stateDiff > 0 && stateDiff < 128) {
            _requestedState = (_requestedState + (stateDiff - 1)) % 256;
            long lag = time - _stateBirthTimes[_requestedState];
            if (lag > _currentLag) {
              _currentLag = lag;
            }
            _requestedState = (_requestedState + 1) % 256;
            if (_requestedState != (buffer[1] & 0xFF)) {
              throw new AssertionError();
            }
          }
        }
      } else if (buffer[0] == REMOTE_MSG_DISCONNECT_ACK) {
        if (debug) {
          Log.v(TAG, "GOT DISCONNECT ACK!");
        }
        if (amt == 1) {
          _connected = false;
          finish();
        }
      } else if (buffer[0] == REMOTE_MSG_DISCONNECT) {
        if (amt == 2) {
          // ignore disconnect msgs for the first second or two in case we're
          // doing a quick disconnect/reconnect and some old ones come
          // trickling in
          {
            String msg;
            if (buffer[1] == BS_REMOTE_ERROR_VERSION_MISMATCH) {
              msg = getString(R.string.versionMismatch);
            } else if (buffer[1] == BS_REMOTE_ERROR_GAME_SHUTTING_DOWN) {
              msg = getString(R.string.gameShutDown);
            } else if (buffer[1] == BS_REMOTE_ERROR_NOT_ACCEPTING_CONNECTIONS) {
              msg = getString(R.string.gameFull);
            } else {
              msg = getString(R.string.disconnected);
            }
            class ToastRunnable implements Runnable {
              private String s;

              private ToastRunnable(String sIn) {
                s = sIn;
              }

              public void run() {
                Context context = getApplicationContext();
                CharSequence text = s;
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
              }
            }
            runOnUiThread(new ToastRunnable(msg));
            _connected = false;
            finish();
          }
        }
      }
    }
  }

}
