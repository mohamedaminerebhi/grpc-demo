import io.grpc.Channel;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple client that requests a greeting from the {@link HelloWorldServer}.
 */
public class HelloWorldClient {
    private static final Logger logger = Logger.getLogger(HelloWorldClient.class.getName());

    private final GreeterGrpc.GreeterBlockingStub blockingStub;

    /** Construct client for accessing HelloWorld server using the existing channel. */
    public HelloWorldClient(Channel channel) {
        // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's responsibility to
        // shut it down.

        // Passing Channels to code makes code easier to test and makes it easier to reuse Channels.
        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    /** Say hello to server. */
    public void greet(String firstName, String lastName, String cin) {
        logger.info("Will try to greet " + firstName + " " + lastName + " with CIN: " + cin + " ...");
        HelloRequest request = HelloRequest.newBuilder()
                                            .setFirstName(firstName)
                                            .setLastName(lastName)
                                            .setCin(cin)
                                            .build();
        HelloReply response;
        try {
            response = blockingStub.sayHello(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
        logger.info("Greeting: " + response.getMessage());
    }

    /**
     * Greet server. If provided, the first element of {@code args} is the name to use in the
     * greeting. The second argument is the target server.
     */
    public static void main(String[] args) throws Exception {
        String firstName = "John";
        String lastName = "Doe";
        String cin = "123456789";

        String target = "localhost:50051";

        // Allow passing in the user and target strings as command line arguments
        if (args.length > 0) {
            if ("--help".equals(args[0])) {
                System.err.println("Usage: [firstName lastName cin [target]]");
                System.err.println("");
                System.err.println("  firstName  The first name of the person to be greeted. Defaults to " + firstName);
                System.err.println("  lastName   The last name of the person to be greeted. Defaults to " + lastName);
                System.err.println("  cin        The CIN of the person to be greeted. Defaults to " + cin);
                System.err.println("  target     The server to connect to. Defaults to " + target);
                System.exit(1);
            }
            firstName = args[0];
        }
        if (args.length > 1) {
            lastName = args[1];
        }
        if (args.length > 2) {
            cin = args[2];
        }
        if (args.length > 3) {
            target = args[3];
        }

        // Create a communication channel to the server, known as a Channel. Channels are thread-safe
        // and reusable. It is common to create channels at the beginning of your application and reuse
        // them until the application shuts down.
        //
        // For the example we use plaintext insecure credentials to avoid needing TLS certificates. To
        // use TLS, use TlsChannelCredentials instead.
        ManagedChannel channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create())
                                      .build();
        try {
            HelloWorldClient client = new HelloWorldClient(channel);
            client.greet(firstName, lastName, cin);
        } finally {
            // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
            // resources the channel should be shut down when it will no longer be used. If it may be used
            // again leave it running.
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
