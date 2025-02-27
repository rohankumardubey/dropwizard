package io.dropwizard.migrations;

import liquibase.Liquibase;
import net.sourceforge.argparse4j.inf.Namespace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

@Execution(SAME_THREAD)
class DbClearChecksumsCommandTest {

    private final DbClearChecksumsCommand<TestMigrationConfiguration> clearChecksums = new DbClearChecksumsCommand<>(
        TestMigrationConfiguration::getDataSource, TestMigrationConfiguration.class, "migrations.xml");

    @Test
    void testRun() throws Exception {
        final Liquibase liquibase = Mockito.mock(Liquibase.class);
        clearChecksums.run(new Namespace(Collections.emptyMap()), liquibase);
        Mockito.verify(liquibase).clearCheckSums();
    }

    @Test
    void testHelpPage() throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        MigrationTestSupport.createSubparser(clearChecksums).printHelp(new PrintWriter(new OutputStreamWriter(out, UTF_8), true));
        assertThat(out.toString(UTF_8)).isEqualToNormalizingNewlines(
            "usage: db clear-checksums [-h] [--migrations MIGRATIONS-FILE]\n" +
                "          [--catalog CATALOG] [--schema SCHEMA]\n" +
                "          [--analytics-enabled ANALYTICS-ENABLED] [file]\n" +
                "\n" +
                "Removes all saved checksums from the database log\n" +
                "\n" +
                "positional arguments:\n" +
                "  file                   application configuration file\n" +
                "\n" +
                "named arguments:\n" +
                "  -h, --help             show this help message and exit\n" +
                "  --migrations MIGRATIONS-FILE\n" +
                "                         the file containing  the  Liquibase migrations for\n" +
                "                         the application\n" +
                "  --catalog CATALOG      Specify  the   database   catalog   (use  database\n" +
                "                         default if omitted)\n" +
                "  --schema SCHEMA        Specify the database schema  (use database default\n" +
                "                         if omitted)\n" +
                "  --analytics-enabled ANALYTICS-ENABLED\n" +
                "                         This turns on analytics  gathering for that single\n" +
                "                         occurrence of a command.\n");
    }
}
