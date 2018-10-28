package com.example.mike.calculator;

import android.view.View;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.Timer;
import java.util.TimerTask;

public class Calculator {

    private TextView outputView;
    private TextView displayView;

    private String cursorString = "|";
    private String input = " ";
    private String output = "";

    private int cursorIndex = 1;

    private TimerTask tsk;
    private Timer timer;

    public void addInput(String s){

        String inputRaw = getInputRaw();
        String lastNumber = inputNumbers()[inputNumbers().length - 1];

        if (s.equals("0") && lastNumber.matches("^0+")) {
            // No number like 0000. Nonsense
            return;
        }
        if ( s.matches("[1-9]") && lastNumber.matches("^0+") ){
            // Replacing numbers such as 001212 with 1212
            replaceLastNumber(s);
            return;
        }
        if ( s.matches( "(sqrt|sin|cos|tan)" ) ){
            if ( inputRaw.matches( "^.+(\\+|-|/|\\*)$" ) ){
                _addInput(s);
                wrapLastWith("");
                return;
            }
            if ( inputRaw.matches("^.+[0-9]$") ){
                replaceLastNumber(s+"("+lastNumber+")");
                return;
            }
        }
        if( s.equals(".") ){
            if ( inputRaw.matches("^.+\\.[0-9]*$") || inputRaw.equals("") || inputRaw.matches("^.+(\\+|-|/|\\*)$")){
                // input is of format "blabla.123" so another . shouldnt do anything
                return;
            }
        }
        if ( s.matches("(\\+|-|/|\\*)") && inputRaw.matches( "^.+\\(-\\)$" ) ){
            // Have unused brackets with (-)
            return;
        }
        if (s.equals("-")){
            if ( inputRaw.matches("^.+(\\+|-|/|\\*)$") && !isLastWrapped() ){
                // minus as a modifier, not a subtraction
                wrapLastWith("-");
                return;
            }
        }

        if ( s.matches("(\\+|-|/|\\*)") && inputRaw.matches("^.+(\\+|-|/|\\*)$") ) {
            //Some action is going on already. cant stack
            return;
        }

        if( s.matches( "(\\+|-|/|\\*)" ) && isLastWrapped() ){
            unwrapLast();
        }

        if (s.equals("=")){
            // Gotta put out result
            Double d = 0.0;

            try{
                d = eval(inputRaw);
            }catch( Exception e ){
                output = e.getMessage();
                refreshOutput();
                return;
            }

            if (d.isInfinite()){
                output = "inf";
                refreshOutput();
                return;
            }

            if ( d == Math.floor(d) ){
                output = String.valueOf(d.intValue());
            }else{
                output = String.valueOf(d);
            }
            refreshOutput();
            return;
        }

        _addInput(s);
    }

    private void _setInput(String s){
        input = s.substring(0, s.length()-cursorIndex+1) + cursorString + s.substring(s.length() - cursorIndex+1, s.length());
        refreshDisplay();
    }

    private String getInputRaw(){
        return input.substring(0, input.length()-cursorIndex) + input.substring(input.length()-cursorIndex+1, input.length());
    }

    private boolean isLastWrapped(){
        return input.endsWith(")");
    }

    public void unwrapLast(){
        if ( !isLastWrapped() ){
            return;
        }
        String inputRaw = getInputRaw();

        cursorIndex = 1;

        _setInput( inputRaw );
    }

    private void wrapLastWith( String mod ){
        if ( isLastWrapped() ){
            return;
        }
        String inputRaw = getInputRaw();

        cursorIndex = 2;

        _setInput( inputRaw + "(" + mod + ")" );
    }

    private void replaceLastNumber(String s){
        String reversedInputRaw = reverseString(getInputRaw());
        String reversedLastNumber = reverseString(inputNumbers()[inputNumbers().length - 1]);

        _setInput( reverseString(reversedInputRaw.replaceFirst( reversedLastNumber, reverseString(s) ) ));
    }

    private String reverseString(String s){
        return new StringBuilder(s).reverse().toString();
    }

    private String[] inputActoins(){
        String inputRaw = getInputRaw();
        return inputRaw.split("\\(?[0-9]+\\.?[0-9]+\\)?");
    }

    private String[] inputNumbers(){
        String inputRaw = getInputRaw();
        return inputRaw.split("\\)?(\\+|-|/|\\*)+\\(?");
    }

    private void _addInput(String s){
        String inputRaw = getInputRaw();
        _setInput( inputRaw.substring(0, inputRaw.length()-cursorIndex+1) + s + inputRaw.substring(inputRaw.length()-cursorIndex+1) );
        refreshDisplay();
    }

    public void setCursor(int pos){
        cursorIndex = pos;
    }

    public void setResultView(View v){
        outputView = (TextView) v;
        refreshOutput();
    }

    public void setDisplayView(View v){
        displayView = (TextView) v;
        refreshDisplay();
        tsk = new TimerTask() {
            boolean stick = true;
            @Override
            public void run() {
                String s = (stick)?cursorString:" ";
                input = input.substring(0, input.length()-cursorIndex) + s + input.substring(input.length()-cursorIndex+1, input.length());

                stick = !stick;
                refreshDisplay();
            }
        };

        timer = new Timer();

        timer.scheduleAtFixedRate(tsk,0,500);
    }

    public void refreshDisplay(){
        displayView.setText(input);
    }

    public void refreshOutput(){
        outputView.setText(output);
    }

    public void clear(){
        cursorIndex = 1;
        input = " ";
        output = "";
        refreshDisplay();
        refreshOutput();
    }

    // Copy pasted from https://stackoverflow.com/questions/3422673/evaluating-a-math-expression-given-in-string-form
    public static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)`
            //        | number | functionName factor | factor `^` factor

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) x /= parseFactor(); // division
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') { // functions
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(startPos, this.pos);
                    x = parseFactor();
                    if (func.equals("sqrt")) x = Math.sqrt(x);
                    else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
                    else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
                    else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
                    else throw new RuntimeException("Unknown function: " + func);
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

                return x;
            }
        }.parse();
    }
}
