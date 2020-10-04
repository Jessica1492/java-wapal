package wapal;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
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

public class App {

    static final By BY_BUTTON_BEGIN = By.xpath("//button[text()='BEGIN']");
    static final By BY_LINK_EPOCH_BEGINS = By.linkText("Tonight your epoch begins.");

    static final By BY_N_IMPREGNATIONS = By.xpath("/html/body/div[4]/div[2]/div/div[2]/thick/strong");

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
            
            logger.debug("Hello Debug");
            logger.info("Hello Info");
            logger.warn("Hello Warning");
            logger.error("Hello Error");
            
            
            // datastructure:
            // list of run results is a tuple of (run R, number of impregs)
            // run R is a list of (index of choice made, number of total options) at each choice point / page
            final List<Tuple<List<Tuple<Integer,Integer>>,Integer>> runs = new ArrayList<>();
            
            try {
                while( true ) {

                    // start a new run
                    {
                        logger.info("Starting Incubus City from "+ic_path);
                        driver.get(ic_path);
            
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
                        logger.debug("Choices available:\n" + MainPage.page.listChoices(driver,wait));

                        // insert a dummy run
                        if ( runs.isEmpty() ) {
                            runs.add( new Tuple<>( Collections.singletonList( new Tuple<>(-1, nlocations ) ), 0 ) );
                        }
                    }

                    // get the newest previous run
                    final List<Tuple<Integer,Integer>> prevRun = runs.get(runs.size()-1).fst;
                    final List<Tuple<Integer,Integer>> curRun = new LinkedList<>();

                    // point to the currently open page
                    Page curPage = MainPage.page;

                    for( int choiceIndex = 0; true; choiceIndex++ ) {
                        // analyze current page
                        final int noptions = curPage.getChoices(driver,wait).size();

                        // make a choice at current point
                        final int choice;
                        if ( choiceIndex < prevRun.size() ) {
                            // choose like in the previous run,
                            // unless all choices have been exhausted to a certain point
                            choice = chooseNextDFS( prevRun, choiceIndex );
                        } else {
                            // enter new territory
                            // assume there is at least 1 choice
                            choice = 0;
                        }

                        // commit choice
                        curRun.add( new Tuple<>(choice,noptions) );
                        
                        // perform the action
                        // wait for the next page to open
                        curPage = curPage.selectChoice( driver, wait, choice );

                        // check if we returned to main page
                        if ( MainPage.page.isShown( driver, wait ) ) {

                            curPage = MainPage.page;
                        }

                        if ( EndOfLifePage.page.isShown( driver, wait ) ) {
                            // end the run
                            final int nimpregs = Page.getImpregnations( wait );
                            runs.add( new Tuple<>( curRun, nimpregs ) );
                            // log run
                            logger.info(String.format("Got %d impregs on run %s.", nimpregs, runAsString( curRun )));
                            // begin next iteration in while loop
                            break;
                        }
                    }
                }
            } catch( IllegalArgumentException exn ) {
                // End-of-life
                System.err.println("Exhausted");
                System.err.println(exn);
            }

            
        } finally {
            driver.quit();
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

        default Page selectChoice( WebDriver driver, WebDriverWait wait, int choice ) {
            final List<WebElement> prevChoices = this.getChoices( driver, wait );
            // remember text to see if anything has changed
            final String prevChoicesText = listChoices( driver, wait );
            
            // perform action
            prevChoices.get( choice ).click();

            // wait for the next page to open
            wait.until(presenceOfElementLocated(By.xpath("//*[@id='passages']//*[contains(@class, 'macro-link')]")));
            // compare new stringified choices with previous stringified choices
            if ( this.listChoices( driver, wait ).equals( prevChoicesText ) ) {
                // TODO: make this more useful in case it actually happens
                throw new RuntimeException("Couldn't progress page");
            }

            // return some handle
            return new SomePage();
        }

        // visible on any page
        static int getImpregnations( WebDriverWait wait ) {
            final String nimpregs = wait.until(presenceOfElementLocated(BY_N_IMPREGNATIONS)).getText();
            return Integer.parseInt(nimpregs);
        }
    }

    static class MainPage implements Page {

        // static instance reference, just so we can conform to the Page pattern
        static final MainPage page = new MainPage();
        static final By BY_MAIN_TITLE = By.xpath("/html/body/div[5]/div/div/p");

        public boolean isShown( WebDriver driver, WebDriverWait wait ) {
            try {
                logger.info("Checking for MainPage...");
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

    static class EndOfLifePage implements Page {

        static final EndOfLifePage page = new EndOfLifePage();
        static final By BY_BUTTON_CONTINUE_TO_EPILOGUE = By.xpath("/html/body/div[5]/div/div/button");

        public boolean isShown( WebDriver driver, WebDriverWait wait ) {
           try {
                logger.info("Checking for EndOfLifePage...");
                return driver.findElement(BY_BUTTON_CONTINUE_TO_EPILOGUE).getText().equals("Continue to Epilogue...");
            } catch( NoSuchElementException exn ) {
                return false;
            }    
        }
    }


    /** Tuple class, because Java has none canonical. */
    static class Tuple<A,B> {
        public final A fst;
        public final B snd;
        public Tuple( A a, B b ) { this.fst=a; this.snd=b; }
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
     * @return the index of the next choice to be made.
     *
     * Performs a depth-first-search.
     * Finds the last position in the choice list with unchosen options available
     * and selects the next.
     * It could be more efficient to calculate the expected run up until this point,
     * but I suppose we're spending much more time waiting on Selenium that iterating this list.
     */
    static int chooseNextDFS( List<Tuple<Integer,Integer>> choices, int off ) throws IllegalArgumentException {

        // iterate backwards from the end
        for( int i = choices.size()-1; i > off; i-- ) {
            final Tuple<Integer,Integer> t_i = choices.get(i);
            // more options available at a later point
            if ( t_i.fst < t_i.snd-1 ) {
                // repeat the choice made at point off to reach the later point
                return choices.get(off).fst;
            }
        }
        // no unchosen options were available at any later point
        // check if current choice point has unchosen options
        final Tuple<Integer,Integer> t_off = choices.get(off);
        if ( t_off.fst < t_off.snd-1 ) {
            return choices.get(off).fst+1;
        }

        final String run_str = runAsString( choices );
        throw new IllegalArgumentException(String.format("Exhausted: all options at offset %d (of %d total) already visited in run %s", off, t_off.snd, run_str));
    }

    static String runAsString( List<Tuple<Integer,Integer>> choices ) {
        return choices.stream()
            .map( e -> String.format("%d/%d", e.fst, e.snd) )
            .collect(Collectors.joining(", ","[","]"));
    }
}
  
