package wapal;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

// TODO: you may encounter the Fence at the first run, and it is completely useless
public class App {

    static final By BY_BUTTON_BEGIN = By.xpath("//button[text()='BEGIN']");
    static final By BY_LINK_EPOCH_BEGINS = By.linkText("Tonight your epoch begins.");

    static final By BY_N_IMPREGNATIONS = By.xpath("/html/body/div[4]/div[2]/div/div[2]/thick/strong");

    // name of the active tab
    static final String ACTIVE_WINDOW_NAME = "IC_window";

    static final Logger logger = LogManager.getRootLogger();

    public static void main(String[] args) {

        final WebDriver driver = new FirefoxDriver();
        final WebDriverWait wait = new WebDriverWait(driver, 10L);
        try {
            // run on a local instance to avoid race conditions wrt image loading
            //driver.get("https://google.com/ncr");
            final String ic_path = System.getProperty("IC_PATH");
            if ( ic_path == null || ic_path.isEmpty() ) {
                System.err.println("You MUST point environment variable IC_PATH to the location of your Incubus City installation.");
                System.exit(1);
            }

            logger.info("Starting Incubus City from "+ic_path);

            // NOTE: reloading the game does not restart it; Twine is too cachy for that
            driver.get(ic_path);
            
            // datastructure:
            // list of run results is a tuple of (run R, number of impregs)
            // run R is a list of (index of choice made, number of total options) at each choice point / page
            final List<Tuple<List<Tuple<Integer,Integer>>,Integer>> runs = new ArrayList<>();
            
            try {
                while( true ) {

                    // store the score "globally" here, because the season-end page may not show it
                    int score = 0;

                    // start a new run
                    {

                        /*
                        // open a new tab to run a season of IC in
                        // TODO: fix org.openqa.selenium.NoSuchWindowException: Browsing context has been discarded
                        try {
                            // switch to leftover tab of the previous season
                            driver.switchTo().window(ACTIVE_WINDOW_NAME);
                            // and close it
                            driver.close();
                        } catch( NoSuchWindowException ignore ) {}
                        
                        // make sure page is reloaded on each iteration by opening in a new tab
                        final String js_open_new_tab = "window.open('"+ic_path+"','"+ACTIVE_WINDOW_NAME+"');";
                        ((JavascriptExecutor)driver).executeScript(js_open_new_tab);

                        // switch to the new tab
                        driver.switchTo().window(ACTIVE_WINDOW_NAME);
                        */
                        
            
                        logger.info("Waiting for SugarCube to finish loading...");
                        // wait until button is clickable
                        // click "BEGIN"
                        wait.until(elementToBeClickable(BY_BUTTON_BEGIN)).click();
            
                        //
                        wait.until(elementToBeClickable(BY_LINK_EPOCH_BEGINS)).click();

                        // TODO: enable all options via Javascript

                        MainPage.page.isShown( driver, wait );
                        // TODO: handle result

                        final int nlocations = MainPage.page.getChoices(driver,wait).size();
                        //logger.debug("Choices available:\n" + MainPage.page.listChoices(driver,wait));

                        // insert a dummy run
                        if ( runs.isEmpty() ) {
                            runs.add( new Tuple<>( Collections.singletonList( new Tuple<>(-1, nlocations ) ), score ) );
                        }
                    }

                    // get the newest previous run
                    final List<Tuple<Integer,Integer>> prevRun = runs.get(runs.size()-1).fst;
                    // track position up until which we do not deviate from the previous run
                    int prevRunMaxValidIndex = prevRun.size();

                    final List<Tuple<Integer,Integer>> curRun = new LinkedList<>();

                    // point to the currently open page
                    Page curPage = MainPage.page;

                    for( int choiceIndex = 0; true; choiceIndex++ ) {
                        // analyze current page
                        final int noptions = curPage.getChoices(driver,wait).size();
                        score = curPage.getScore( wait );

                        // calculate a choice at current point
                        final int choice;
                        if ( choiceIndex < prevRunMaxValidIndex ) {
                            // point has been visited before
                            // choose like in the previous run,
                            // unless all later choices have been exhausted
                            final Either<Integer,Integer> eitherChoice = chooseNextDFS( prevRun, choiceIndex );
                            if ( eitherChoice.isRight() ) {
                                choice = eitherChoice.getRight();
                            } else {
                                choice = eitherChoice.getLeft();
                                prevRunMaxValidIndex = choiceIndex-1;
                            }

                        } else {
                            // enter new territory
                            // assume there is at least 1 choice
                            choice = 0;
                        }

                        // commit choice
                        curRun.add( new Tuple<>(choice,noptions) );
                        
                        try {
                            // perform the action
                            // wait for the next page to open
                            curPage = curPage.selectChoiceAndWait( driver, wait, choice );
                            
                            try {
                                // update score after making the choice
                                score = curPage.getScore( wait );
                            } catch( NumberFormatException ignore ) {
                                // do nothing if score is not found; use score last read
                            }

                        } catch ( TimeoutException exn ) {
                            // check again if season has ended
                            // because the fade-in can be slow

                            if ( EndOfSeasonPage.page.isShown( driver, wait ) ) {
                                //final int nimpregs = Page.getImpregnations( wait );
                                runs.add( new Tuple<>( curRun, score ) );
                                // log run
                                logger.info("Got {} impregs on run {}", score, runAsString( curRun ));
                                // no need to click the button
                                // begin next iteration in while loop
                                curPage.restart( driver, wait );
                                break;
                            } else {
                                logger.error("Ran out of choices before season end for run {}", runAsString( curRun ));
                                throw exn;
                            }
                        } catch( RestartRequiredException exn ) {

                            logger.warn("Restarting stalled run {}", runAsString( curRun ));
                            curPage.restart( driver, wait );
                            break;
                        }

                        // check if we returned to main page
                        if ( MainPage.page.isShown( driver, wait ) ) {

                            curPage = MainPage.page;
                        }

                    }
                }
            } catch( SearchSpaceExhaustedException exn ) {
                // End-of-life
                logger.info("Search Space Exhausted");
                logger.info(exn);
            }

            
        } finally {
            driver.quit();
        }
    }

    /** If you encounter the Fence on a run,
      * the iteration can get stuck after you run out of money.
      * In this case a restart needs to be signaled.
      */
    static class RestartRequiredException extends Exception {
        public RestartRequiredException( String message ) {
            super( message );
        }
    }

    static class SearchSpaceExhaustedException extends Exception {
        public SearchSpaceExhaustedException( String message ) {
            super( message );
        }
    }

    
    interface Page {
        boolean isShown( WebDriver driver, WebDriverWait wait );

        /** May need some filtering, e.g. for Chrono Sands */
        default List<WebElement> getChoices( WebDriver driver, WebDriverWait wait ) {
            return driver
                .findElement(By.id("passages"))
                .findElements(By.className("macro-link"));

        }

        default String listChoices( WebDriver driver, WebDriverWait wait ) {
            final List<String> choices = this.getChoices( driver, wait )
                .stream()
                .map( WebElement::getText )
                .collect(Collectors.toList());
            return choices.stream().collect(Collectors.joining("\n- ","- ",""));
        }

        default Page selectChoiceAndWait( WebDriver driver, WebDriverWait wait, int choice ) throws TimeoutException, RestartRequiredException {
            final List<WebElement> prevChoices = this.getChoices( driver, wait );
            // remember text to see if anything has changed
            final String prevChoicesText = listChoices( driver, wait );
            
            // perform action
            final WebElement choiceElement = prevChoices.get( choice );
            logger.debug("Choosing {}", choiceElement.getText());
            choiceElement.click();
        

            // wait for the next page to open
            wait.until(presenceOfElementLocated(By.xpath("//*[@id='passages']//*[contains(@class, 'macro-link')]")));
            // compare new stringified choices with previous stringified choices
            if ( this.listChoices( driver, wait ).equals( prevChoicesText ) ) {
                // TODO: make this more useful in case it actually happens
                throw new RestartRequiredException("Couldn't progress page");
            }

            // return some handle
            return new SomePage();
        }

        // visible on most pages, but not Season-End
        default int getScore( WebDriverWait wait ) {
            final String nimpregs = wait.until(presenceOfElementLocated(BY_N_IMPREGNATIONS)).getText();
            return Integer.parseInt(nimpregs);
        }

        static final By BY_BUTTON_RESTART = By.xpath("//a[text()='Restart']");

        default void restart( WebDriver driver, WebDriverWait wait ) {
            final WebElement buttonRestart = driver.findElement(BY_BUTTON_RESTART);
            
            /*if ( ! buttonRestart.getText().equals("Restart") ) {
                logger.error("Restart button not found at xpath");
                // proceed to click whatever we found anyway
            }*/

            buttonRestart.click();

            // wait for modal confirmation dialog to open & click it
            wait.until(presenceOfElementLocated(By.id("restart-ok"))).click();
        }
    }

    static class MainPage implements Page {

        // static instance reference, just so we can conform to the Page pattern
        static final MainPage page = new MainPage();
        static final By BY_MAIN_TITLE = By.xpath("/html/body/div[5]/div/div/p");

        public boolean isShown( WebDriver driver, WebDriverWait wait ) {
            try {
                //logger.debug("Checking for MainPage...");
                return driver.findElement(BY_MAIN_TITLE).getText().equals("In your mind's eye, fertile wombs glitter like gems all around you. You sense...");
            } catch( NoSuchElementException exn ) {
                return false;
            }
        }

    }

    static class SomePage implements Page {
        public boolean isShown( WebDriver driver, WebDriverWait wait ) {
            return true;
        }
    }

    static class EndOfSeasonPage implements Page {

        static final EndOfSeasonPage page = new EndOfSeasonPage();
        static final By BY_BUTTON_CONTINUE_TO_EPILOGUE = By.xpath("//button[text()='Continue to Epilogue...']");

        public boolean isShown( WebDriver driver, WebDriverWait wait ) {
           try {
                //logger.debug("Checking for EndOfSeasonPage...");
                return driver.findElement(BY_BUTTON_CONTINUE_TO_EPILOGUE).getText().equals("Continue to Epilogue...");
            } catch( NoSuchElementException exn ) {
                return false;
            }    
        }
    }


    /** Tuple class, because Java has none canonical. */
    public static class Tuple<A,B> {
        public final A fst;
        public final B snd;
        public Tuple( A a, B b ) { this.fst=a; this.snd=b; }
    }

    interface Either<A,B> {

        static <AA,BB> Left<AA,BB> left( AA a ) { return new Left<>(a); }
        static <AA,BB> Right<AA,BB> right( BB b ) { return new Right<>(b); }

        default boolean isLeft() { return ! this.isRight(); }
        boolean isRight();

        A getLeft();
        B getRight();

    }

    static class Left<A,B> implements Either<A,B> {
        final A a;
        public Left( A a ) { this.a=a; }

        public boolean isRight() { return false; }

        public A getLeft() { return this.a; }
        public B getRight() { throw new IllegalStateException("Left has no Right"); }

    }
    static class Right<A,B> implements Either<A,B> {
        final B b;
        public Right( B b) { this.b=b; }

        public boolean isRight() { return true; }

        public A getLeft() { throw new IllegalStateException("Right has no Left"); }
        public B getRight() { return this.b; }

    }

    

    /**
     * Given:
     * @param choices a list representing a complete previous run,
     * where each tuple contains:
     * - fst: the index of the choice made
     * - snd: the maximum index possible at that choice point (dependent on previous choices)
     * And given:
     * @param off the current number of choices already made (i.e. the offset in the list).
     * Then this function will:
     * @return the index of the next choice to be made. Right if the choice follows what 'choices' contained, Left if it deviates
     *
     * Performs a depth-first-search.
     * Finds the last position in the choice list with unchosen options available
     * and selects the next.
     * It could be more efficient to calculate the expected run up until this point,
     * but I suppose we're spending much more time waiting on Selenium that iterating this list.
     */
    public static Either<Integer,Integer> chooseNextDFS( List<Tuple<Integer,Integer>> choices, int off ) throws SearchSpaceExhaustedException {

        // iterate backwards from the end
        for( int i = choices.size()-1; i > off; i-- ) {
            final Tuple<Integer,Integer> t_i = choices.get(i);
            // more options available at a later point
            if ( t_i.fst < t_i.snd-1 ) {
                // repeat the choice made at point 'off' to reach the later point
                return Either.right( choices.get(off).fst );
            }
        }
        // no unchosen options were available at any later point
        // check if current choice point has unchosen options
        final Tuple<Integer,Integer> t_off = choices.get(off);
        if ( t_off.fst < t_off.snd-1 ) {
            return Either.left( choices.get(off).fst+1 );
        } else {
            final String run_str = runAsString( choices );
            throw new SearchSpaceExhaustedException(String.format("Exhausted: all options at offset %d (of %d total) already visited in run %s", off, t_off.snd, run_str));
        }
    }

    static String runAsString( List<Tuple<Integer,Integer>> choices ) {
        return choices.stream()
            // abbreviate "1/1" to "-"
            .map( e -> e.snd == 1 ? "-" : String.format("%d/%d", e.fst+1, e.snd) )
            .collect(Collectors.joining(", ","[","]"));
    }
}
  
