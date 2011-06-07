/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.util;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLException;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.util.NIOUtils;
import java.nio.Buffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

/**
 * Generic homogen transformer class
 * @author ochafik
 * @param <B> NIO buffer class that represents the data consumed and produced by this transformer
 * @param <A> primitive array class that represents the data consumed and produced by this transformer
 */
public interface Transformer<T, B extends Buffer, A> {
	CLContext getContext();
    A transform(CLQueue queue, A input, boolean inverse);
    B transform(CLQueue queue, B input, boolean inverse);
    CLEvent transform(CLQueue queue, CLBuffer<T> input, CLBuffer<T> output, boolean inverse, CLEvent... eventsToWaitFor) throws CLException;
    int computeOutputSize(int inputSize);
    
    public abstract class AbstractTransformer<T, B extends Buffer, A> implements Transformer<T, B, A> {
        protected final Class<T> primitiveClass;
        protected final CLContext context;

        public AbstractTransformer(CLContext context, Class<T> primitiveClass) {
            this.primitiveClass = primitiveClass;
            this.context = context;
        }
        
        public CLContext getContext() { return context; }

        public int computeOutputSize(int inputSize) {
            return inputSize;
        }

        public A transform(CLQueue queue, A input, boolean inverse) {
            return (A)NIOUtils.getArray(transform(queue, (B)NIOUtils.wrapArray(input), inverse));
        }
        public B transform(CLQueue queue, B in, boolean inverse) {
            int inputSize = in.capacity();
            int length = inputSize / 2;

            CLBuffer<T> inBuf = context.createBuffer(CLMem.Usage.Input, in, true); // true = copy
            CLBuffer<T> outBuf = context.createBuffer(CLMem.Usage.Output, primitiveClass, computeOutputSize(inputSize));

            CLEvent dftEvt = transform(queue, inBuf, outBuf, inverse);
            inBuf.release();
            
            B out = (B)outBuf.read(queue, dftEvt);
            outBuf.release();
            return out;
        }

    }
}