/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities;

/**
 *
 * @author travis
 */
public class Quicksort {
    
     static public void quicksort(int m, int n, int[] data, String[] urls) {
        if (m < n) {
            int i = m;
            int j = n + 1;
            int k = data[m];
            for (;;) {
                do {
                    i++;
                } while (data[i] < k);
                do {
                    j--;
                } while (data[j] > k);
                if (i < j) {
                    swap(i, j, data, urls);
                } else {
                    break;
                }
            }
            swap(m, j, data, urls);
            quicksort(m, j - 1, data, urls);
            quicksort(j + 1, n, data, urls);
        }
    }

    static private void swap(int m, int j, int[] data, String[] urls) {
        int temp = data[m];
        String tempURL = urls[m];
        data[m] = data[j];
        urls[m] = urls[j];
        data[j] = temp;
        urls[j] = tempURL;
    }
}
