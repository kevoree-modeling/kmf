package org.kmf.generator;

import org.junit.Test;

import java.io.File;

public class GeneratorTest {

    @Test
    public void test() throws Exception {
        Generator gen = new Generator();
        gen.scan(new File("/Users/duke/dev/kmf/generator/src/test/resources"));
        gen.generate(new File("/Users/duke/dev/kmf/generator/src/test/resources/out"));
    }

}
