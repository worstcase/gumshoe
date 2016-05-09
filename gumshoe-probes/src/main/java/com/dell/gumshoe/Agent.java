package com.dell.gumshoe;

import sun.misc.IoTrace;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/** install our version of IoTrace to capture socket and file I/O activity
 */
public class Agent {
    private static final String CLASS_FILENAME = "/sun.misc.IoTrace.class";
    private static boolean WAS_INSTALLED = false;

    public static void premain(String args, Instrumentation inst) throws Exception {
        final ProbeManager probeManager = ProbeManager.getInstance();
        probeManager.initialize();
        if(probeManager.isUsingIoTrace()) {
            new Agent().installIoTraceHook(inst);
        }
        WAS_INSTALLED = true;
    }

    public static boolean isAgentInstalled() {
        return WAS_INSTALLED;
    }

    private  void installIoTraceHook(Instrumentation inst) throws IOException {
        final byte[] alternate = getAlternateBytecode();
        if(alternate==null) {
            System.out.println("GUMSHOE ERROR: failed to locate IoTrace hook");
            return;
        }
        try {
            inst.redefineClasses(new ClassDefinition(IoTrace.class, alternate));
            if(Boolean.getBoolean("gumshoe.verbose")) {
                System.out.println("GUMSHOE: installed IoTrace hook");
            }
        } catch(Exception e) {
            System.out.println("GUMSHOE ERROR: failed to install IoTrace hook");
            e.printStackTrace();
        }
    }

    /** loop over classpath */
    private byte[] getAlternateBytecode() throws IOException {
        final InputStream in = ProbeManager.class.getResourceAsStream(CLASS_FILENAME);
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final byte[] block = new byte[8192];
        int len;
        while((len=in.read(block))>-1) {
            buffer.write(block, 0, len);
        }
        return buffer.toByteArray();
    }

    private static byte[] getAlternateBytecodeOLD() throws IOException {
        final String[] classpath = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
        for(String entry : classpath) {
            final File file = new File(entry);
            if( ! file.canRead()) continue;

            if(file.isFile()) {
                final JarInputStream in = new JarInputStream(new FileInputStream(file));
                try {
                    final byte[] contents = getFromJar(in);
                    if(contents!=null) { return contents; }
                } finally {
                    in.close();
                }
            } else {
                // this case happens during development
                if(file.isDirectory()) {
                    final File classFile = new File(file, CLASS_FILENAME);
                    if(classFile.isFile()) {
                        return getFromFile(classFile);
                    }
                }
            }
        }
        return null;
    }

    private static byte[] getFromJar(JarInputStream jarIn) throws IOException {
        JarEntry entry;
        while((entry = jarIn.getNextJarEntry())!=null) {
            if(CLASS_FILENAME.equals(entry.getName())) {
                return getFromStream(jarIn, entry.getSize());
            }
        }
        return null;
    }

    private static byte[] getFromFile(File file) throws IOException {
        final InputStream in = new BufferedInputStream(new FileInputStream(file));
        try {
            return getFromStream(in, file.length());
        } finally {
            in.close();
        }
    }

    private static byte[] getFromStream(InputStream in, long size) throws IOException {
        final byte[] contents = new byte[(int)size];
        int pos = 0;
        int len;
        while(size-pos>0 && (len=in.read(contents, pos, (int)size-pos))!=-1) {
            pos+=len;
        }
        return contents;
    }
}
