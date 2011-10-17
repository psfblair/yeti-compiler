package yeti.lang.compiler;

import org.junit.*;
import mockit.*;

import java.io.FileInputStream;

public class SourceReaderTests {
    @Mocked FileInputStream fileInputStream;

    @Test
    public void testThat () {

    }

    // Test that if we set an override, the override is called with a pair formed from the args,
    // the filename passed in in the array is modified
    // and the character array of the result is returned

    // Test that if we set an override, the override is called with a pair formed from the args,
    // and if the override returns an empty string, the name returned from the function is
    // used to load the file in the conventional way, and the filename passed into the array is still modified
    // with the name returned from the function

    // Test that if we do not set an override,
}
