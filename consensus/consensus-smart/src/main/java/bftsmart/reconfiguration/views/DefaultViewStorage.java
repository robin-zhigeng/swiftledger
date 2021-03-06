/**
 * Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bftsmart.reconfiguration.views;

import com.higgschain.trust.consensus.bftsmartcustom.started.custom.SpringUtil;
import com.higgschain.trust.consensus.bftsmartcustom.started.custom.config.SmartConfig;

import java.io.*;

/**
 * The type Default view storage.
 *
 * @author eduardo
 */
public class DefaultViewStorage implements ViewStorage {

    private String path = "";

    /**
     * Instantiates a new Default view storage.
     */
    public DefaultViewStorage() {
        String sep = System.getProperty("file.separator");
        SmartConfig smartConfig = SpringUtil.getBean(SmartConfig.class);
        path = smartConfig.getDefaultDir() + sep + "config";
        File f = new File(path);
        if (!f.exists()) {
            f.mkdirs();
        }
        path = path + sep + "currentView";
    }

    @Override public boolean storeView(View view) {
        if (!view.equals(readView())) {
            File f = new File(path);
            try {
                ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
                oos.writeObject(view);
                oos.flush();
                oos.close();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    @Override public View readView() {
        File f = new File(path);
        if (!f.exists()) {
            return null;
        }
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
            View ret = (View)ois.readObject();
            ois.close();

            return ret;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get bytes byte [ ].
     *
     * @param view the view
     * @return the byte [ ]
     */
    public byte[] getBytes(View view) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(4);
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(view);
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets view.
     *
     * @param bytes the bytes
     * @return the view
     */
    public View getView(byte[] bytes) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (View)ois.readObject();
        } catch (Exception e) {
            return null;
        }
    }
}
