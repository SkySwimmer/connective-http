import org.asf.connective.standalone.modules.IConnectiveModule;

public class TestModule implements IConnectiveModule {

	@Override
	public String moduleID() {
		return "test";
	}

	@Override
	public String version() {
		return "1.0.0.0";
	}

	@Override
	public void init() {
	}

}
