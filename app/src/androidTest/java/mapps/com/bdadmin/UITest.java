package mapps.com.bdadmin;

import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.ComponentNameMatchers.hasShortClassName;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.intent.matcher.IntentMatchers.toPackage;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class UITest {

    @Rule
    public IntentsTestRule<LoginActivity> loginActivityRule = new IntentsTestRule<>(LoginActivity.class);

    @Test
    public void emptyFields() {
        onView(withId(R.id.submit)).perform(click());
        onView(withId(R.id.username_label))
                .check(matches(withText("User name\nThis field is required.")));
        onView(withId(R.id.password_label))
                .check(matches(withText("Password\nThis field is required.")));
    }


    @Test
    public void incorrectLogin() {
        onView(withId(R.id.username)).perform(typeText("abcd"), closeSoftKeyboard());
        onView(withId(R.id.password)).perform(typeText("abcd"), closeSoftKeyboard());
        onView(withId(R.id.submit)).perform(click());
        onView(withId(R.id.nfe))
                .check(matches(withText("\nIncorrect username or password")));
    }

    @Test
    public void correctLogin() {
        String ADMIN_USERNAME = "test";
        onView(withId(R.id.username)).perform(typeText(ADMIN_USERNAME), closeSoftKeyboard());
        String ADMIN_PASS = "hellotest123";
        onView(withId(R.id.password)).perform(typeText(ADMIN_PASS), closeSoftKeyboard());
        onView(withId(R.id.submit)).perform(click());

        intended(allOf(
                hasComponent(hasShortClassName(".MainActivity")),
                toPackage("mapps.com.bdadmin"),
                hasExtra("username", ADMIN_USERNAME),
                hasExtra("password", ADMIN_PASS)));
    }
}
