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
public class ComplexTemp {

    protected double real;
    protected double imag;

    public ComplexTemp(double real, double imag) {
        this.real = real;
        this.imag = imag;
    }
    
    @Override
    public ComplexTemp clone() {
        ComplexTemp clone = new ComplexTemp(this.real, this.imag);
        return clone;
    }

    // return a string representation of the invoking Complex object
    @Override
    public String toString() {
        if (imag == 0) {
            return real + "";
        }
        if (real == 0) {
            return imag + "i";
        }
        if (imag < 0) {
            return real + " - " + (-imag) + "i";
        }
        return real + " + " + imag + "i";
    }

    // return abs/modulus/magnitude and angle/phase/argument
    public double abs() {
        return Math.hypot(real, imag);
    }  // Math.sqrt(re*re + im*im)

    public double phase() {
        return Math.atan2(imag, real);
    }  // between -pi and pi

    // return a new Complex object whose value is (this + b)
    public ComplexTemp plus(ComplexTemp b) {
        double re = this.real + b.real;
        double im = this.imag + b.imag;
        return new ComplexTemp(re, im);
    }

    // return a new Complex object whose value is (this - b)
    public ComplexTemp minus(ComplexTemp b) {
        double re = this.real - b.real;
        double im = this.imag - b.imag;
        return new ComplexTemp(re, im);
    }

    // return a new Complex object whose value is (this * b)
    public ComplexTemp times(ComplexTemp b) {
        double re = this.real * b.real - this.imag * b.imag;
        double im = this.real * b.imag + this.imag * b.real;
        return new ComplexTemp(re, im);
    }

    // scalar multiplication
    // return a new object whose value is (this * alpha)
    public ComplexTemp times(double alpha) {
        return new ComplexTemp(alpha * real, alpha * imag);
    }

    // return a new Complex object whose value is the conjugate of this
    public ComplexTemp conjugate() {
        return new ComplexTemp(real, -imag);
    }

    // return a new Complex object whose value is the reciprocal of this
    public ComplexTemp reciprocal() {
        double scale = real * real + imag * imag;
        return new ComplexTemp(real / scale, -imag / scale);
    }

    // return the real or imaginary part
    public double getReal() {
        return real;
    }

    public double getImag() {
        return imag;
    }

    // return a / b
    public ComplexTemp divides(ComplexTemp b) {
        return this.times(b.reciprocal());
    }

    // return a new Complex object whose value is the complex exponential of this
    public ComplexTemp exp() {
        return new ComplexTemp(Math.exp(real) * Math.cos(imag), Math.exp(real) * Math.sin(imag));
    }

    // return a new Complex object whose value is the complex sine of this
    public ComplexTemp sin() {
        return new ComplexTemp(Math.sin(real) * Math.cosh(imag), Math.cos(real) * Math.sinh(imag));
    }

    // return a new Complex object whose value is the complex cosine of this
    public ComplexTemp cos() {
        return new ComplexTemp(Math.cos(real) * Math.cosh(imag), -Math.sin(real) * Math.sinh(imag));
    }

    // return a new Complex object whose value is the complex tangent of this
    public ComplexTemp tan() {
        return sin().divides(cos());
    }

    // a static version of plus
    public static ComplexTemp plus(ComplexTemp a, ComplexTemp b) {
        double re = a.real + b.real;
        double im = a.imag + b.imag;
        ComplexTemp sum = new ComplexTemp(re, im);
        return sum;
    }
}
