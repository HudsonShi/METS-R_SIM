package mets_r.facility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;

import mets_r.ContextCreator;
import mets_r.GlobalVariables;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import repast.simphony.space.gis.ShapefileLoader;

public class ZoneContext extends FacilityContext<Zone> {
	public ZoneContext() {
		super("ZoneContext");
		ContextCreator.logger.info("ZoneContext creation");
		GeographyParameters<Zone> geoParams = new GeographyParameters<Zone>();
		Geography<Zone> zoneGeography = GeographyFactoryFinder.createGeographyFactory(null)
				.createGeography("ZoneGeography", this, geoParams);
		// Read in the data and add to the context and geography
		File zoneFile = null;
		ShapefileLoader<Zone> zoneLoader = null;
		try {
			zoneFile = new File(GlobalVariables.ZONES_SHAPEFILE);
			URI uri = zoneFile.toURI();
			zoneLoader = new ShapefileLoader<Zone>(Zone.class, uri.toURL(), zoneGeography, this);
			BufferedReader br = new BufferedReader(new FileReader(GlobalVariables.ZONE_CSV));
			String line = br.readLine();
			String[] result = line.split(",");
			if(result.length < 3) {
				ContextCreator.logger.error("Missing fields in Zone configuration, a proper one should contain (Name, externalID, Capacity)");
			}
			int int_id = 0; // Start with ID = 0
			while (zoneLoader.hasNext()) {
				line = br.readLine();
				result = line.split(",");
				Zone zone = zoneLoader.nextWithArgs(int_id, (int) Math.round(Double.parseDouble(result[2]))); // Using customize parameters
				this.put(int_id, zone);
				int_id += 1;
				zone.setCoord(zoneGeography.getGeometry(zone).getCentroid().getCoordinate());
			}
			br.close();
			ContextCreator.logger.info("Zone generated, total number: " + (int_id + 1));
		} catch (Exception e) {
			ContextCreator.logger.error("Malformed URL exception or file not exists when reading zone sshapefile.");
			e.printStackTrace();
		}

	}
}
