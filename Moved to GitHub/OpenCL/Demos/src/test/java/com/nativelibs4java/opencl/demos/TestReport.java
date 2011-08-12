/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nativelibs4java.opencl.demos;
import com.nativelibs4java.opencl.*;

import com.nativelibs4java.opencl.demos.hardware.HardwareReport;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Test that HardwareReport runs without exception
 * @author ochafik
 */
public class TestReport {

    @Test
    public void runReport() {
        for (CLPlatform platform : JavaCL.listPlatforms()) {
            List<Map<String, Object>> list = HardwareReport.listInfos(platform);
            HardwareReport.toHTML(list);
        }
    }
}
