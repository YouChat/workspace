package com.youchat.web.controller.car;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.youchat.dto.biz.FlexiGridJson;
import com.youchat.dto.biz.Result;
import com.youchat.entity.car.Car;
import com.youchat.entity.car.CarDegree;
import com.youchat.entity.car.CarImage;
import com.youchat.service.car.ICarDegreeService;
import com.youchat.service.car.ICarImageService;
import com.youchat.service.car.ICarService;
import com.youchat.service.travelGuide.ITravelGuideService;
import com.youchat.utils.DateUtils;
import com.youchat.utils.Regex;
import com.youchat.web.controller.tools.UploadFileController;

/**
 * car controller.
 * @author lusar
 *
 */
@Controller
public class CarController {

	@Autowired
	private ICarService carService;

	@Autowired
	private ICarImageService carImageSerive;
	
	@Autowired
	private ICarDegreeService carDegreeService;

	@Autowired
	private ITravelGuideService travelGuideService;
	
	
	/**
	 * get all car info.
	 * @return
	 */
	@RequestMapping("/car/getAllCars.do")
	@ResponseBody
	public String getAllCars(){
		Result result=new Result();
		List<Car> cars = carService.getAllCars();
		result.setSuccess(true);
		result.putObject("total", cars.size());
		result.putObject("cars", cars);
		return result.toJsonString();
	}
	

	/**
	 * get all car info.
	 * @return
	 */
	@RequestMapping("/car/getAllCarsForFlexi.do")
	@ResponseBody
	public String getAllCarsForFlexi(){
		List<Car> cars = carService.getAllCars();
		
		for(Car car : cars){
			CarDegree cardegree = carDegreeService.selectById(Integer.valueOf(car.getCarDegreeId()));
			if(cardegree!=null){
				car.setCarDegreeName(cardegree.getName());
			}
			List<CarImage> carImages = carImageSerive.getCarImages(car.getId());
			if(!carImages.isEmpty()){
				car.setCarImageUrl(carImages.get(0).getAddress());
			}
		}
		FlexiGridJson flexigrid = new FlexiGridJson(1,cars.size(), cars);
		return flexigrid.toJson();
	}

	/**
	 * delete car by id.
	 * @param carId
	 * @return
	 */
	@RequestMapping("/v/w/car/deleteCarById.do")
	@ResponseBody
	public String deleteCar(String carId){
		Result result = new Result();
		if(! Regex.regex(Regex.sint, carId)){
			return result.setErrorMsg("类型不正确").toJsonString();
		}
		boolean flag = carService.deleteCarById(Integer.valueOf(carId));
		result.setSuccess(flag);
		result.setMsg(flag ? "删除成功" : "删除失败");
		return result.toJsonString();
	}

	/**
	 * add new car.
	 * @param car
	 * @return
	 */
	@RequestMapping("/v/w/car/addCar.do")
	@ResponseBody
	public String addNewCar(HttpServletRequest request,
											Car car,
										MultipartFile   file){
		
		Result result = UploadFileController.uploadFile(request, "car", file);
		if(! result.getSuccess()){ return result.toJsonString(); }
		boolean flag = carService.addCar(car);
		result.setSuccess(flag);
		result.setMsg(flag ? "创建成功" : "创建失败");
		
		
		if(result.getSuccess()){
			CarImage carImage = new CarImage();
			carImage.setCarID(car.getId());
			carImage.setAddress(result.getDatas().get("filePath").toString());
			carImage.setName(car.getName());
			flag = carImageSerive.addCarImage(carImage);
		}
		
		return result.toJsonString();
	}
	

	@RequestMapping("/car/getCarsByTravleGuideId.do")
	@ResponseBody
	public String getCarsByTravleGuide(Integer travelGuideId){
		Result result = new Result();
		if(travelGuideId==null){
			result.setSuccess(false);
			result.setMsg("获取车辆信息时候，travelGuideId参数不能为空！");
		}else{
			List<Car> cars = carService.getCarsByTravelGuideId(travelGuideId);
			if(cars==null){
				result.setSuccess(false);
				result.setMsg("没有车辆信息！");
			}else{
				for(Car car:cars){
					List<CarImage> carImages = carImageSerive.getCarImages(car.getId());
					if(!carImages.isEmpty()){
						car.setCarImageUrl(carImages.get(0).getAddress());
					}
				}
				result.setSuccess(true);
				result.setMsg("查询成功！");
				result.putObject("cars", cars);
			}
		}
		return result.toJsonString();
	}
	/**
	 * 获取旅游攻略对应的路线的各个档次的车的价格
	 * @param travelGuideId
	 * @param date
	 * @return
	 */
	@RequestMapping("/car/getPriceByTravleGuideId.do")
	@ResponseBody
	public String getPriceByTravleGuideId(Integer travelGuideId,String date){
		Result result = new Result();
		if(travelGuideId==null||date==null||"".equals(date)){
			result.setSuccess(false);
			result.setMsg("获取价格时候，travelGuideId和date参数不能为空！");			
		}else{
			java.util.Date d = DateUtils.yyyy_MM_dd2Date(date);
			if(d==null){
				result.setSuccess(false);
				result.setMsg("获取价格时候，date参数不对！");	
			}else{
				Map<String,Object> prices = travelGuideService.getPriceByTravelGuideId(travelGuideId,d);
				if(prices==null||prices.size()==0){
					result.setSuccess(false);
					result.setMsg("没有改攻略的价格信息！");		
				}else{
					result.setMsg("查询成功！");
					result.setSuccess(true);
					result.addMapDatas(prices);
				}
			}

		}
		return result.toJsonString();
	}
}