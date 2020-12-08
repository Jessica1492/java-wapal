package wapal.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * First page when beginning a new Day.
 */
public class MainPage implements Page {

    // static instance reference, just so we can conform to the Page pattern
    public static final MainPage page = new MainPage();
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