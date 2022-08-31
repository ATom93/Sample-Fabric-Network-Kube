package org.example;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

public class GatewayManager {
    private final int peerPort = 30751;
    private final String peerHostname = "peer1-org1-com";

    private ManagedChannel grpcChannel;

    public GatewayManager(String peerAddress, X509Certificate TLSCert) throws SSLException {
        grpcChannel = NettyChannelBuilder.forAddress(peerAddress,peerPort)/*forTarget(peerAddress+":"+peerPort)*/
                .sslContext(GrpcSslContexts.forClient().trustManager(TLSCert).build())
                .overrideAuthority(peerHostname)
                .build();
    };

    public Gateway.Builder getGatewayBuilder(Identity identity, Signer signer) throws SSLException {
        Gateway.Builder gatewayBuilder = Gateway.newInstance()
                .identity(identity)
                .signer(signer)
                .connection(grpcChannel);
        return gatewayBuilder;
    }

    public void closeGRPCChannel() throws InterruptedException {
        grpcChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

}
