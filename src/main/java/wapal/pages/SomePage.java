package wapal.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * SomePage can be reached from any Page.
 * if you are at this page, then you don't know where you're going to.
 */
public class SomePage implements Page {
    public boolean isShown( WebDriver driver, WebDriverWait wait ) {
        return true;
    }
}