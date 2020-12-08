package wapal.pages;

import wapal.RestartRequiredException;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Interface for all Pages on the right-hand-side main screen.
 */
public interface Page {
    boolean isShown( WebDriver driver, WebDriverWait wait );

    static final Logger logger = LogManager.getRootLogger();

    static final By BY_N_IMPREGNATIONS = By.xpath("/html/body/div[4]/div[2]/div/div[2]/thick/strong");

    /** May need some filtering, e.g. for Chrono Sands */
    default List<WebElement> getChoices( WebDriver driver, WebDriverWait wait ) {
        return driver
            .findElement(By.id("passages"))
            .findElements(By.className("macro-link"));

    }

    /** Render a debug String */
    default String listChoices( WebDriver driver, WebDriverWait wait ) {
        final List<String> choices = this.getChoices( driver, wait )
            .stream()
            .map( WebElement::getText )
            .collect(Collectors.toList());
        return choices.stream().collect(Collectors.joining("\n- ","- ",""));
    }

    /**
     * Choose
     * @param choice the j-th option in dialogue
     * @param point_i at choice-point i
     */
    default Page selectChoiceAndWait( WebDriver driver, WebDriverWait wait, int point_i, int choice ) throws TimeoutException, RestartRequiredException {

        final List<WebElement> prevChoices = this.getChoices( driver, wait );
        // remember text to see if anything has changed
        final String prevChoicesText = listChoices( driver, wait );
        
        // perform action
        final WebElement choiceElement;
        try { choiceElement = prevChoices.get( choice ); }
        catch( IndexOutOfBoundsException exn ) {
            logger.error("Failed trying to choose {} from {}", choice, prevChoicesText );
            throw exn;
        }
        logger.debug("choice {} ({}/{}): {}", point_i, choice+1, prevChoices.size(), choiceElement.getText());
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