package com.bridj.objc;

import com.bridj.NativeObject;
import com.bridj.Pointer;

@com.bridj.ann.Runtime(ObjectiveCRuntime.class)
public class ObjCObject extends NativeObject {

    ObjCObject type;

    public native <T extends ObjCObject> Pointer<T> create();

    public ObjCObject(Pointer<? extends NativeObject> peer) {
        super(peer);
    }

    public ObjCObject() {
        super();
    }

    public ObjCObject(int constructorId, Object... args) {
        super(constructorId, args);
    }

}
