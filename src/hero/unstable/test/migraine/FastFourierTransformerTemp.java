/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hero.unstable.test.migraine;

/**
 *
 * @author José Luis Risco Martín <jlrisco at ucm.es>
 */
public class FastFourierTransformerTemp {

    public FastFourierTransformerTemp() {
    }

    public ComplexTemp[] completeWithZero(ComplexTemp[] x) {
        int powerOfTwo = 1;
        long maxPowerOfTo = 2147483648L;
        while (powerOfTwo < x.length && powerOfTwo < maxPowerOfTo) {
            powerOfTwo *= 2;
        }
        ComplexTemp[] xx = new ComplexTemp[powerOfTwo];
        for (int i = 0; i < x.length; ++i) {
            xx[i] = x[i];
        }
        for (int i = x.length; i < powerOfTwo; ++i) {
            xx[i] = new ComplexTemp(0, 0);
        }
        return xx;
    }

    public ComplexTemp[] fft(ComplexTemp[] initialArray) {
        ComplexTemp[] x = completeWithZero(initialArray);
        int n = x.length;

        // base case
        if (n == 1) {
            return new ComplexTemp[]{x[0]};
        }

        // fft of even terms
        ComplexTemp[] even = new ComplexTemp[n / 2];
        for (int k = 0; k < n / 2; k++) {
            even[k] = x[2 * k];
        }
        ComplexTemp[] q = fft(even);

        // fft of odd terms
        ComplexTemp[] odd = even;  // reuse the array
        for (int k = 0; k < n / 2; k++) {
            odd[k] = x[2 * k + 1];
        }
        ComplexTemp[] r = fft(odd);

        // combine
        ComplexTemp[] y = new ComplexTemp[n];
        for (int k = 0; k < n / 2; k++) {
            double kth = -2 * k * Math.PI / n;
            ComplexTemp wk = new ComplexTemp(Math.cos(kth), Math.sin(kth));
            y[k] = q[k].plus(wk.times(r[k]));
            y[k + n / 2] = q[k].minus(wk.times(r[k]));
        }
        return y;
    }

    // compute the inverse FFT of x[], assuming its length is a power of 2
    public ComplexTemp[] ifft(ComplexTemp[] x) {
        int N = x.length;
        ComplexTemp[] y = new ComplexTemp[N];

        // take conjugate
        for (int i = 0; i < N; i++) {
            y[i] = x[i].conjugate();
        }

        // compute forward FFT
        y = fft(y);

        // take conjugate again
        for (int i = 0; i < N; i++) {
            y[i] = y[i].conjugate();
        }

        // divide by N
        for (int i = 0; i < N; i++) {
            y[i] = y[i].times(1.0 / N);
        }

        return y;

    }

    // compute the circular convolution of x and y
    public ComplexTemp[] cconvolve(ComplexTemp[] x, ComplexTemp[] y) {

        // should probably pad x and y with 0s so that they have same length
        // and are powers of 2
        if (x.length != y.length) {
            throw new RuntimeException("Dimensions don't agree");
        }

        int N = x.length;

        // compute FFT of each sequence
        ComplexTemp[] a = fft(x);
        ComplexTemp[] b = fft(y);

        // point-wise multiply
        ComplexTemp[] c = new ComplexTemp[N];
        for (int i = 0; i < N; i++) {
            c[i] = a[i].times(b[i]);
        }

        // compute inverse FFT
        return ifft(c);
    }

    // compute the linear convolution of x and y
    public ComplexTemp[] convolve(ComplexTemp[] x, ComplexTemp[] y) {
        ComplexTemp ZERO = new ComplexTemp(0, 0);

        ComplexTemp[] a = new ComplexTemp[2 * x.length];
        for (int i = 0; i < x.length; i++) {
            a[i] = x[i];
        }
        for (int i = x.length; i < 2 * x.length; i++) {
            a[i] = ZERO;
        }

        ComplexTemp[] b = new ComplexTemp[2 * y.length];
        for (int i = 0; i < y.length; i++) {
            b[i] = y[i];
        }
        for (int i = y.length; i < 2 * y.length; i++) {
            b[i] = ZERO;
        }

        return cconvolve(a, b);
    }

    // display an array of Complex numbers to standard output
    public void show(ComplexTemp[] x, String title) {
        System.out.println(title);
        System.out.println("-------------------");
        for (ComplexTemp x1 : x) {
            System.out.println(x1);
        }
        System.out.println();
    }
}
