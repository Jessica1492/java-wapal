package wapal;

import wapal.pages.*;

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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;

// TODO: you may encounter the Fence at the first run, and it is completely useless
public class App {

    static final By BY_BUTTON_BEGIN = By.xpath("//button[text()='BEGIN']");
    static final By BY_LINK_EPOCH_BEGINS = By.linkText("Tonight your epoch begins.");

    static final By BY_N_IMPREGNATIONS = By.xpath("/html/body/div[4]/div[2]/div/div[2]/thick/strong");

    // name of the active tab
    static final String ACTIVE_WINDOW_NAME = "IC_window";

    static final Path RUN_LOG = Path.of( "./run.log" );

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
            final List<Tuple<Run,Integer>> runs = Continuity.recoverRunsFromLog( RUN_LOG );
            
            try {
                next_season: while( true ) {

                    // store the score scope-"globally" here, because the season-end page may not show it
                    int score = 0;

                    // start a new run
                    {
            
                        logger.info("Waiting for SugarCube to finish loading...");
                        // wait until button is clickable
                        // click "BEGIN"
                        wait.until(elementToBeClickable(BY_BUTTON_BEGIN)).click();
            
                        //
                        wait.until(elementToBeClickable(BY_LINK_EPOCH_BEGINS)).click();

                        MainPage.page.isShown( driver, wait );

                        final int nlocations = MainPage.page.getChoices(driver,wait).size();

                        // insert a dummy run
                        if ( runs.isEmpty() ) {
                            runs.add( new Tuple<>( Run.singleton( nlocations ), score ) );
                        }
                    }

                    // goal: run-with-side-effects until EndOfSeasonPage opens

                    // precompute run as deep as possible from previous runs
                    final Run curRun = nextPrefixDFS( runs );

                    logger.debug("Starting next run {}", Run.asString(curRun));

                    // point to the currently open page
                    Page curPage = MainPage.page;

                    // actual length of current may change because of decisions made
                    for( int choiceIndex = 0; true; choiceIndex++ ) {
                        // analyze current page
                        final int noptions = curPage.getChoices(driver,wait).size();

                        // update score
                        score = curPage.getScore( wait );

                        // calculate a choice at current point
                        final int choice;
                        if( choiceIndex < curRun.size() ) {
                            // proceed as predicted
                            choice = curRun.get( choiceIndex ).fst;
                        } else {
                            // enter new territory
                            // assume there is at least 1 choice
                            choice = 0;
                            // commit choice
                            curRun.add( new Tuple<>(choice,noptions) );
                        }
                        
                        try {
                            // perform the action
                            // wait for the next page to open
                            curPage = curPage.selectChoiceAndWait( driver, wait, choiceIndex, choice );
                            
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
                                Continuity.logSuccess( curRun, score );
                                // no need to click the button
                                // begin next iteration in while loop
                                curPage.restart( driver, wait );
                                
                                continue next_season;
                            } else {
                                logger.error("Ran out of choices before season end for run {}", Run.asString( curRun ));
                                throw exn;
                            }
                        } catch( RestartRequiredException exn ) {

                            logger.warn("Restarting stalled run {}", Run.asString( curRun ));
                            curPage.restart( driver, wait );
                            
                            continue next_season;
                        } catch( IndexOutOfBoundsException exn ) {
                            // a previous run was not reproducible, because something changed
                            logger.error("Run contained non-reproducible junction {} because something changed: {}", choiceIndex, Run.asString( curRun ));

                            curPage.restart( driver, wait );
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

    static class SearchSpaceExhaustedException extends Exception {
        public SearchSpaceExhaustedException( String message ) {
            super( message );
        }
    }

    public static Run nextPrefixDFS( List<Tuple<Run,Integer>> runs ) throws SearchSpaceExhaustedException {

        // get the newest previous run
        final Run prevRun = runs.get(runs.size()-1).fst;

        // TODO: check if run was seen before
        return nextPrefixDFS( prevRun );
    }

    /**
     * Given a previous
     * @param run e.g. [2/3, 1/2, 1/1], will
     * @return the prefix with an increment of the deepest position, i.e. [2/3, 2/2]
     * The call can the extend this run into the unknown.
     */
    public static Run nextPrefixDFS( Run run ) throws SearchSpaceExhaustedException {

        if( run.isEmpty() ) {
            return run;
        }

        // iterate backwards from the end
        int i;
        Tuple<Integer,Integer> t_i = null; // assert: run.size()>0, therefore below loop is run at least once
        for( i = run.size()-1; i >= 0; i-- ) {
            t_i = run.get(i);
            // more options are avaiable at this point
            if( t_i.fst < t_i.snd-1 ) {
                break;
            }
        }
        // i now contains the maximum stable prefix length
        if ( i < 0 ) {
            throw new SearchSpaceExhaustedException(String.format("Exhausted: all options at offset 0 already visited in run %s", Run.asString( run ) ));
        }

        // collect into mutable list up until before the junction point
        final Run res = run.stream().limit( i ).collect( Run.collect() );
        
        // increase by 1 at junction point
        // append new junction to stable prefix
        final Tuple<Integer,Integer> plus1 = new Tuple<>( t_i.fst+1, t_i.snd );
        res.add( plus1 );

        return res;
    }

    /**
     * A list of tuples describing "current-choice/total-choices".
     * The value for "current-choice" starts at offset 0 internally,
     * but is represented as offset-1 for display through method {@link #asString}.
     */
    interface Run extends List<Tuple<Integer,Integer>> {

        static class RunArray extends ArrayList<Tuple<Integer,Integer>> implements Run {}

        static Run empty() {
            return new RunArray();
        }

        static Run singleton( int nlocations ) {
            final Run res = new RunArray();
            res.add( new Tuple<>(-1, nlocations ) );
            return res;
        }

        /** Render a run datastructure as a string*/
        static String asString( Run choices ) {
            return choices.stream()
                // abbreviate "1/1" to "-"
                .map( e -> e.snd == 1 ? "-" : String.format("%d/%d", e.fst+1, e.snd) )
                .collect(Collectors.joining(", ","[","]"));
        }

        /** @return if parsing is successful, return Right<Run>, otherwise Left<String> */
        static Either<String,Run> fromString( String str ) {
            if ( ! str.startsWith("[") ) return Either.left(str);
            if ( ! str.endsWith("]") ) return Either.left(str); 
            final Run run = Arrays.stream( str.substring(1, str.length()-1).split(", *") )
                // expand the shorthand "-" to "1/1"
                .map( e -> "-".equals(e) ? "1/1" : e )
                // expect pattern: "current/total"
                .map( e -> e.split("/") )
                // subtract 1 from choice offset
                .map( xy -> new Tuple<>( Integer.parseInt( xy[0] )-1, Integer.parseInt( xy[1] ) ) )
                .collect( Run.collect() );
            return Either.right( run );
        }

        static Collector<Tuple<Integer,Integer>,Run,Run> collect() {
            // combiner (3rd argument) needs a wrapper, as opposed to its inlined variant:
            // final Run res = run.stream().limit( i ).collect( Run.RunArray::new, Run::add, Run::addAll );
            return Collector.<Tuple<Integer,Integer>,Run>of( RunArray::new, Run::add, (as,bs) -> { as.addAll(bs); return as; } );
        }

        static Run ofSteps( Tuple<Integer,Integer>... steps ) {
            return Arrays.stream( steps ).collect( Run.collect() );
        }
    }

    interface Continuity {

        static final String LOG_FORMAT = "Got {} impregs on run {}";
        static final Pattern LOG_PATTERN = Pattern.compile( ".* Got ([0-9]+) impregs on run (.*)" );
                    
        static void logSuccess( Run run, int score ) {
            logger.info( LOG_FORMAT, score, Run.asString( run ));
        }

        static List<Tuple<Run,Integer>> recoverRunsFromLog( Path logfile ) {
  
            try {
                //return
                final java.util.stream.Stream<Tuple<Run,Integer>> strm =
                 Files.lines( logfile )
                     // subject all lines to the matcher
                     .map( LOG_PATTERN::matcher )
                     // keep only lines that match the pattern
                     .filter( Matcher::matches )
                     // extract score and run description
                     .map( mtc ->
                        Run.fromString(mtc.group(2))
                           .mapRight( run -> new Tuple( run, Integer.parseInt(mtc.group(1)) ) ) )
                     // log failures to parse
                     .map( ei -> ei.mapLeft( malformedStr -> { logger.warn("Failed to parse run description %s", malformedStr); return malformedStr;} ) )
                     // unwrap results for which parsing succeded
                     .filter( Either::isRight )
                     .map( Either::getRight )
                ; return strm
                    // TODO annotate type information to method call instead of storing the intermediate. dont cast!
                    .collect( Collectors.toList() );
            } catch( IOException exn ) {
                logger.warn("Failed to recover from log {}", logfile);
                return new ArrayList<>();
            }
        }

    }

}