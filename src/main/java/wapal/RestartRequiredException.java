package wapal;

/** If you encounter the Fence on a run,
  * the iteration can get stuck after you run out of money.
  * In this case a restart needs to be signaled.
  */
public class RestartRequiredException extends Exception {
    public RestartRequiredException( String message ) {
        super( message );
    }
}