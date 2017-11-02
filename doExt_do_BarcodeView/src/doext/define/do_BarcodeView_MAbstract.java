package doext.define;

import core.object.DoProperty;
import core.object.DoUIModule;
import core.object.DoProperty.PropertyDataType;


public abstract class do_BarcodeView_MAbstract extends DoUIModule{

	protected do_BarcodeView_MAbstract() throws Exception {
		super();
	}
	
	/**
	 * 初始化
	 */
	@Override
	public void onInit() throws Exception{
        super.onInit();
        //注册属性
    	this.registProperty(new DoProperty("scanArea", PropertyDataType.String, "", false));
	}
}