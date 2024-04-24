package com.jp.wasabeef.glide.transformations.internal;

import android.graphics.Bitmap;

/**
 * Copyright (C) 2015 Wasabeef
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

public class FastBlur {

    // Ajoutez un constructeur privé pour cacher le constructeur public implicite
    private FastBlur() {
        // Constructeur privé pour empêcher l'instanciation de la classe.
        // Cette classe ne devrait pas être instanciée car elle contient uniquement des méthodes statiques.
    }

    public static Bitmap blur(Bitmap sentBitmap, int radius, boolean canReuseInBitmap) {
        // Copie l'image source si nécessaire
        Bitmap bitmap = canReuseInBitmap ? sentBitmap : sentBitmap.copy(sentBitmap.getConfig(), true);
        // Vérifie le rayon de flou
        if (radius < 1) {
            return null;
        }
    
        // Obtient les dimensions de l'image
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = width * height;
        int[] pixels = new int[size];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
    
        // Applique l'algorithme de flou
        stackBlur(pixels, width, height, radius);
    
        // Remplace les pixels de l'image par ceux floutés
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    
        return bitmap;
    }
    
    private static void stackBlur(int[] pixels, int width, int height, int radius) {
        int[] blurredPixels = new int[width * height];
        System.arraycopy(pixels, 0, blurredPixels, 0, width * height);
        
        // Applique l'algorithme de flou
        StackBlur.blurNatively(blurredPixels, width, height, radius);
    
        // Copie les pixels floutés dans le tableau original
        System.arraycopy(blurredPixels, 0, pixels, 0, width * height);
    }
    
}