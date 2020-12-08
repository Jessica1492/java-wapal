package wapal.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * This Goal can be reached from an EndOfDayPage.
 * It can be reached from any EndOfDayPage, when the remaining Day counter turns 0.
 */
public class EndOfSeasonPage implements Page {

    public static final EndOfSeasonPage page = new EndOfSeasonPage();
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