package ua.zaskarius.keycloak.plugins.radius.providers;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static ua.zaskarius.keycloak.plugins.radius.providers.AuthRadiusHandlerSPI.AUTH_RADIUS_SPI;

public class AuthRadiusHandlerSpiTest {
    private AuthRadiusHandlerSPI radiusProviderSpi =
            new AuthRadiusHandlerSPI();

    @Test
    public void testMethods() {
        assertEquals(radiusProviderSpi.getProviderClass(),
                IRadiusAuthHandlerProvider.class);
        assertEquals(radiusProviderSpi.getProviderFactoryClass(),
                IRadiusAuthHandlerProviderFactory.class);
        assertEquals(radiusProviderSpi.getName(), AUTH_RADIUS_SPI);
        assertFalse(radiusProviderSpi.isInternal());
    }
}
