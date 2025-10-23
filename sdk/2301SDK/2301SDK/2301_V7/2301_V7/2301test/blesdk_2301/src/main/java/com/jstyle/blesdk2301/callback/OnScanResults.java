package com.jstyle.blesdk2301.callback;


import com.jstyle.blesdk2301.model.Device;

public interface OnScanResults {
  void Success(Device date);
  void Fail(int code);
}
