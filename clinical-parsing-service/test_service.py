"""
简单的服务测试脚本
用于验证服务是否正常工作
"""
import asyncio
import sys
from app.services.parsing_service import ClinicalParsingService
from app.models.request import ClinicalParsingRequest

async def test_service():
    """测试服务"""
    print("=" * 50)
    print("测试病例理解服务")
    print("=" * 50)
    
    # 创建服务实例
    service = ClinicalParsingService()
    
    # 测试用例1：症状识别
    print("\n测试用例1：症状识别")
    print("-" * 50)
    request1 = ClinicalParsingRequest(
        userId="test_user",
        sessionId="test_session",
        text="我最近胸口闷，走几步就喘"
    )
    
    try:
        response1 = await service.parse(request1)
        print(f"识别到 {len(response1.concepts)} 个概念")
        for concept in response1.concepts:
            print(f"  - {concept.originalText} -> {concept.normalizedSymptom} (置信度: {concept.confidence:.2f})")
        print(f"提取到 {len(response1.structuredData.symptoms)} 个症状")
    except Exception as e:
        print(f"错误: {e}")
        import traceback
        traceback.print_exc()
    
    # 测试用例2：疾病识别
    print("\n测试用例2：疾病识别")
    print("-" * 50)
    request2 = ClinicalParsingRequest(
        userId="test_user",
        sessionId="test_session",
        text="我之前有高血压，现在正在吃阿司匹林"
    )
    
    try:
        response2 = await service.parse(request2)
        print(f"识别到 {len(response2.concepts)} 个概念")
        for concept in response2.concepts:
            print(f"  - {concept.originalText} -> {concept.normalizedSymptom} (类型: {concept.conceptType})")
        print(f"提取到 {len(response2.structuredData.medicalHistory)} 个既往史")
        print(f"提取到 {len(response2.structuredData.medications)} 个药物")
    except Exception as e:
        print(f"错误: {e}")
        import traceback
        traceback.print_exc()
    
    # 测试用例3：歧义表达
    print("\n测试用例3：歧义表达判定")
    print("-" * 50)
    request3 = ClinicalParsingRequest(
        userId="test_user",
        sessionId="test_session",
        text="我最近胸痛"
    )
    
    try:
        response3 = await service.parse(request3)
        print(f"识别到 {len(response3.concepts)} 个概念")
        if response3.ambiguousExpressions:
            print(f"检测到 {len(response3.ambiguousExpressions)} 个歧义表达")
            for amb in response3.ambiguousExpressions:
                print(f"  - {amb.text}: {amb.suggestedQuestions[0] if amb.suggestedQuestions else '无'}")
        else:
            print("未检测到歧义表达")
    except Exception as e:
        print(f"错误: {e}")
        import traceback
        traceback.print_exc()
    
    print("\n" + "=" * 50)
    print("测试完成")
    print("=" * 50)

if __name__ == "__main__":
    asyncio.run(test_service())

