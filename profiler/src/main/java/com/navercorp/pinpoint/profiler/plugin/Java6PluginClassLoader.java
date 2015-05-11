/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.profiler.plugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Jongho Moon
 * @author emeroad
 */
public class Java6PluginClassLoader extends URLClassLoader {

    private final ReentrantLock lock = new ReentrantLock();

    public Java6PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        ClassLoader parent = getParent();
        
        while (!lock.tryLock()) {
            try {
                try {
                    this.wait();
                } catch (IllegalMonitorStateException e) {
                    try {
                        parent.wait();
                    } catch (IllegalMonitorStateException e2) {
                        // should sleep?
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted", e);
            }
        }
        
        Class<?> c = null;

        try {

            synchronized (this) {
                c = findLoadedClass(name);

                if (c == null) {
                    try {
                        c = parent.loadClass(name);
                    } catch (ClassNotFoundException e) {

                    }

                    if (c == null) {
                        c = findClass(name);
                    }
                }

                if (resolve) {
                    resolveClass(c);
                }
            }
            

        } finally {
            lock.unlock();
        }

        synchronized (this) {
            this.notify();
        }
        
        synchronized (parent) {
            parent.notify();
        }
        return c;
    }
}
