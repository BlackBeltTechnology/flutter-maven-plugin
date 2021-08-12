package hu.blackbelt.flutter.maven.plugin;

public interface Log {
	
    /**
     * Send a message to the user in the <b>debug</b> error level.
     *
     * @param content
     */
    void debug( CharSequence content );

    /**
     * Send a message (and accompanying exception) to the user in the <b>debug</b> error level.
     * <br/>
     * The error's stacktrace will be output when this error level is enabled.
     *
     * @param content
     * @param error
     */
    void debug( CharSequence content, Throwable error );

    /**
     * Send an exception to the user in the <b>debug</b> error level.
     * <br/>
     * The stack trace for this exception will be output when this error level is enabled.
     *
     * @param error
     */
    void debug( Throwable error );


    /**
     * Send a message to the user in the <b>info</b> error level.
     *
     * @param content
     */
    void info( CharSequence content );

    /**
     * Send a message (and accompanying exception) to the user in the <b>info</b> error level.
     * <br/>
     * The error's stacktrace will be output when this error level is enabled.
     *
     * @param content
     * @param error
     */
    void info( CharSequence content, Throwable error );

    /**
     * Send an exception to the user in the <b>info</b> error level.
     * <br/>
     * The stack trace for this exception will be output when this error level is enabled.
     *
     * @param error
     */
    void info( Throwable error );

    /**
     * Send a message to the user in the <b>warn</b> error level.
     *
     * @param content
     */
    void warn( CharSequence content );

    /**
     * Send a message (and accompanying exception) to the user in the <b>warn</b> error level.
     * <br/>
     * The error's stacktrace will be output when this error level is enabled.
     *
     * @param content
     * @param error
     */
    void warn( CharSequence content, Throwable error );

    /**
     * Send an exception to the user in the <b>warn</b> error level.
     * <br/>
     * The stack trace for this exception will be output when this error level is enabled.
     *
     * @param error
     */
    void warn( Throwable error );

    /**
     * Send a message to the user in the <b>error</b> error level.
     *
     * @param content
     */
    void error( CharSequence content );

    /**
     * Send a message (and accompanying exception) to the user in the <b>error</b> error level.
     * <br/>
     * The error's stacktrace will be output when this error level is enabled.
     *
     * @param content
     * @param error
     */
    void error( CharSequence content, Throwable error );

    /**
     * Send an exception to the user in the <b>error</b> error level.
     * <br/>
     * The stack trace for this exception will be output when this error level is enabled.
     *
     * @param error
     */
    void error( Throwable error );


}
