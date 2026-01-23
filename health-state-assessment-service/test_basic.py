"""
基础功能测试脚本
用于快速验证服务功能
"""
import asyncio
from app.models.request import HealthStateAssessmentRequest
from app.services.health_state_assessment import HealthStateAssessmentService


async def test_basic():
    """基础功能测试"""
    service = HealthStateAssessmentService()
    
    # 测试1：无症状用户（健康筛查路径）
    print("=" * 50)
    print("测试1：无症状用户（健康筛查路径）")
    print("=" * 50)
    request1 = HealthStateAssessmentRequest(
        userId="user001",
        userInput="我想做个体检",
        basicInfo={"age": 30, "gender": "男"}
    )
    result1 = await service.assess(request1)
    print(f"工作态: {result1.workMode}")
    print(f"风险等级: {result1.riskLevel}")
    print(f"路径选择: {result1.entryAssessment.pathSelected if result1.entryAssessment else 'N/A'}")
    print()
    
    # 测试2：有症状用户（症状诊断路径）
    print("=" * 50)
    print("测试2：有症状用户（症状诊断路径）")
    print("=" * 50)
    request2 = HealthStateAssessmentRequest(
        userId="user002",
        userInput="我最近胸痛",
        basicInfo={"age": 45, "gender": "男"},
        symptoms=["胸痛"]
    )
    result2 = await service.assess(request2)
    print(f"工作态: {result2.workMode}")
    print(f"风险等级: {result2.riskLevel}")
    print(f"路径选择: {result2.entryAssessment.pathSelected if result2.entryAssessment else 'N/A'}")
    print()
    
    # 测试3：危险信号（退出流程）
    print("=" * 50)
    print("测试3：危险信号（退出流程）")
    print("=" * 50)
    request3 = HealthStateAssessmentRequest(
        userId="user003",
        userInput="我胸痛，还出汗，感觉喘不上气",
        basicInfo={"age": 50, "gender": "男"},
        symptoms=["胸痛", "出汗", "呼吸困难"]
    )
    result3 = await service.assess(request3)
    print(f"工作态: {result3.workMode}")
    print(f"风险等级: {result3.riskLevel}")
    print(f"危险信号: {result3.redFlags}")
    print(f"路径选择: {result3.entryAssessment.pathSelected if result3.entryAssessment else 'N/A'}")
    print()
    
    print("=" * 50)
    print("测试完成！")
    print("=" * 50)


if __name__ == "__main__":
    asyncio.run(test_basic())

