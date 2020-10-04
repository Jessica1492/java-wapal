package wapal;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class App {

    static final By BY_BUTTON_BEGIN = By.xpath("//button[text()='BEGIN']");
    static final By BY_LINK_EPOCH_BEGINS = By.linkText("Tonight your epoch begins.");

    static final By BY_N_IMPREGNATIONS = By.xpath("/html/body/div[4]/div[2]/div/div[2]/thick/strong");

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
            System.out.println("Starting Incubus City from "+ic_path);
            driver.get(ic_path);
            
            System.out.println("Waiting for SugarCube to finish loading...");
            // wait until button is clickable
            // click "BEGIN"
            wait.until(elementToBeClickable(BY_BUTTON_BEGIN)).click();
            
            //
            wait.until(elementToBeClickable(BY_LINK_EPOCH_BEGINS)).click();

            MainPage.page.check( driver, wait );
            // TODO: handle result

            System.out.println("Passages available:\n" + MainPage.page.listPassages(driver,wait));

            // TODO: check for End-of-Life
        } finally {
            driver.quit();
        }
    }

    
    interface Page {
        boolean check( WebDriver driver, WebDriverWait wait );

        default void dumpImpregnations( WebDriverWait wait ) {
            final String nimpregs = wait.until(presenceOfElementLocated(BY_N_IMPREGNATIONS)).getText();
            System.out.println("Impregnations: "+ nimpregs);
        }
    }

    static class MainPage implements Page {

        // static instance reference, just so we can conform to the Page pattern
        static final MainPage page = new MainPage();
        static final By BY_MAIN_TITLE = By.xpath("/html/body/div[5]/div/div/p");

        public boolean check( WebDriver driver, WebDriverWait wait ) {
            try {
                return driver.findElement(BY_MAIN_TITLE).getText().equals("In your mind's eye, fertile wombs glitter like gems all around you. You sense...");
            } catch( NoSuchElementException exn ) {
                return false;
            }
        }

        public String listPassages( WebDriver driver, WebDriverWait wait ) {
            final List<WebElement> passages = driver.findElement(By.id("passages")).findElements(By.className("macro-link"));
            return passages.stream()
                .map( WebElement::getText )
                .collect(Collectors.joining("\n- ","- ",""));
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

        final String run_str = choices.stream()
            .map( e -> String.format("%d/%d", e.fst, e.snd) )
            .collect(Collectors.joining(", ","[","]"));
        throw new IllegalArgumentException(String.format("Exhausted: all options at offset %d (of %d total) already visited in run %s", off, t_off.snd, run_str));
    }
}
  
