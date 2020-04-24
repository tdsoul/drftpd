package org.drftpd.slave;

import org.drftpd.common.network.AsyncCommandArgument;
import org.drftpd.common.network.AsyncResponse;
import org.drftpd.slave.protocol.BasicHandler;
import org.drftpd.slave.protocol.NodePath;
import org.drftpd.slave.protocol.SlaveProtocolCentral;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BasicHandlerTest {
    final int chunkSize = 1000;
    private Collection<List<NodePath>> processDirectory(String rootPath) throws IOException {
        final AtomicInteger counter2 = new AtomicInteger();
        long start = System.currentTimeMillis();
        Path p = Paths.get(rootPath);
        try (Stream<Path> stream = Files.walk(p, Integer.MAX_VALUE)) {
            Collection<List<NodePath>> values = stream.parallel()
                    .filter(f -> !f.toAbsolutePath().toString().equals(rootPath))
                    .map(d -> new NodePath(d, rootPath))
                    .collect(Collectors.groupingBy(NodePath::getPath)).values();
            System.out.println("Listing with java8 stream path done in " + (System.currentTimeMillis() - start) + "ms");
            return values;
        }
    }

    @Test
    public void testRemerge() throws Exception {

        List<String> paths = Arrays.asList("e:\\Games");
        paths.parallelStream().forEach(s -> {
            try {
                processDirectory(s);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // final AtomicInteger counter = new AtomicInteger();
        // List<String> paths = Arrays.asList("e:\\Games");
        // Collection<List<NodePath>> collect = paths.parallelStream().map(path -> {
        //     Path p = Paths.get(path);
        //     try (Stream<Path> stream = Files.walk(p, Integer.MAX_VALUE)) {
        //         return stream.parallel()
        //                 .filter(f -> !f.toAbsolutePath().toString().equals(path))
        //                 .map(d -> new NodePath(d, path));
        //                 //.collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize)).values();
        //     } catch (IOException e) {
        //         throw new RuntimeException(e);
        //     }
        // //}).collect(Collectors.toList());
        // }).flatMap(Collection::stream).collect(Collectors.toList());
        // System.out.println("Listing with java8 stream path done in " + (System.currentTimeMillis() - start) + "ms");




        long start = System.currentTimeMillis();
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("slave.conf.dist");
        Properties properties = new Properties();
        properties.load(resourceAsStream);
        Slave slave = new Slave(properties, true);
        SlaveProtocolCentral slaveProtocolCentral = new SlaveProtocolCentral(slave);
        BasicHandler basicHandler = new BasicHandler(slaveProtocolCentral);
//
        AsyncCommandArgument asyncCommandArgument = new AsyncCommandArgument("ff", "remerge", new String[]{"/", "false", "0", "0", "false"});
        AsyncResponse asyncResponse = basicHandler.handleRemerge(asyncCommandArgument);
        long end = System.currentTimeMillis();
        System.out.println("Listing with remerge implementation done in " + (end - start) + "ms");
    }
}
