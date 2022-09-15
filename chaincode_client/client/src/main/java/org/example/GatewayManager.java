package org.example;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;

import javax.net.ssl.SSLException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

public class GatewayManager {
    private final int peerPort = 30751;
    private final String peerHostname = "peer1-org1-com";

    private ManagedChannel grpcChannel;

    /**
     * Creates a new GatewayManager instance.
     *
     * @param peerAddress Address of the peer to connect to
     * @param TLSCert TLS certificate to authenticate on the peer node
     * @throws SSLException
     */
    public GatewayManager(String peerAddress, X509Certificate TLSCert) throws SSLException {
        grpcChannel = NettyChannelBuilder.forAddress(peerAddress,peerPort)
                .sslContext(GrpcSslContexts.forClient().trustManager(TLSCert).build())
                .overrideAuthority(peerHostname)
                .build();
    };

    /**
     * Get a Gateway builder with connection configuration
     *
     * @param identity Identity of the user to be used for the Gateway builder
     * @param signer Signer object used to sign transactions generated from a private key
     * @return a Gateway builder
     * @throws SSLException
     */
    public Gateway.Builder getGatewayBuilder(Identity identity, Signer signer) throws SSLException {
        Gateway.Builder gatewayBuilder = Gateway.newInstance()
                .identity(identity)
                .signer(signer)
                .connection(grpcChannel);
        return gatewayBuilder;
    }

    /**
     * Close the GRPC channel for connection of the Fabric Gateway component
     * @throws InterruptedException
     */
    public void closeGRPCChannel() throws InterruptedException {
        grpcChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

}
