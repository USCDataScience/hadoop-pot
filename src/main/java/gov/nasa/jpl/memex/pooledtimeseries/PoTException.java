package gov.nasa.jpl.memex.pooledtimeseries;

/**
 * Created by Aditya on 10/29/15.
 */
public class PoTException extends Exception {
    //Parameterless Constructor
    private PoTException() {}

    //Constructor that accepts a message
    public PoTException(String message)
    {
        super(message);
        this.message = message;
    }

    public String message;
}
