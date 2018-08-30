package com.baidu.openrasp.tool.Annotation;

import java.io.File;
import java.io.FileFilter;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @program openrasp
 * @description: 扫描特定包名下的含有特定的注解的类
 * @author: anyang
 * @create: 2018/08/29 21:04
 */
public class AnnotationScanner {

    public static Set<Class> getClassWithAnnotation(String packageName) {
        Set<Class> classes = new LinkedHashSet<Class>();
        Set<Class>  res = new LinkedHashSet<Class>();
        String newPackageName = packageName.replace('.', '/');
        Enumeration<URL> urls;
        try {
            urls = Thread.currentThread().getContextClassLoader().getResources(newPackageName);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    findAndAddClass(packageName, filePath, classes);
                } else if ("jar".equals(protocol)) {
                    JarFile jar;
                    jar = ((JarURLConnection) url.openConnection()).getJarFile();
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (name.startsWith("/")) {
                            name = name.substring(1);
                        }
                        if (name.startsWith(newPackageName)) {
                            int index = name.lastIndexOf('/');
                            if ((index != -1)) {
                                if (name.endsWith(".class") && !entry.isDirectory()) {
                                    packageName = name.substring(0, index).replace('/', '.');
                                    String className = name.substring(packageName.length() + 1, name.length() - 6);
                                    classes.add(Class.forName(packageName + '.' + className));

                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (Class clazz : classes){
            if (clazz.getAnnotation(HookAnnotation.class)!=null){
                res.add(clazz);
            }
        }
        return res;

    }

    private static void findAndAddClass(String packageName, String packagePath, Set<Class> classes) {
        File baseDir = new File(packagePath);
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            return;
        }
        File[] fileList = baseDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return (file.isDirectory()) || (file.getName().endsWith(".class"));
            }
        });
        if (fileList != null && fileList.length > 0) {
            for (File file : fileList) {
                if (file.isDirectory()) {
                    findAndAddClass(packageName + "." + file.getName(), file.getAbsolutePath(), classes);
                } else {
                    String className = file.getName().substring(0,
                            file.getName().length() - 6);
                    try {
                        classes.add(Thread.currentThread().getContextClassLoader().loadClass(packageName + '.' + className));
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }


    }
}
