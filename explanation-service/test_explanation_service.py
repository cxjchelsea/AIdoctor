"""
解释服务独立测试脚本
用于测试解释服务的各个功能，不依赖完整诊断流程
"""
import asyncio
import httpx
import json
from typing import Dict, Any

# 解释服务地址
EXPLANATION_SERVICE_URL = "http://localhost:8089"
# 诊断服务地址（用于获取CDP数据）
DIAGNOSIS_SERVICE_URL = "http://localhost:8084"

async def test_conclusion_package_with_real_cdp(cdp_id: str):
    """使用真实的CDP ID测试"""
    print(f"\n{'='*60}")
    print(f"测试1: 使用真实CDP ID测试终点结论包生成")
    print(f"CDP ID: {cdp_id}")
    print(f"{'='*60}\n")
    
    async with httpx.AsyncClient(timeout=30.0) as client:
        try:
            # 调用解释服务
            url = f"{EXPLANATION_SERVICE_URL}/api/v1/explain/conclusion-package"
            request_data = {
                "cdpId": cdp_id
            }
            
            print(f"请求URL: {url}")
            print(f"请求数据: {json.dumps(request_data, indent=2, ensure_ascii=False)}")
            print("\n发送请求...")
            
            response = await client.post(url, json=request_data)
            
            print(f"\n响应状态码: {response.status_code}")
            print(f"响应内容:")
            print(json.dumps(response.json(), indent=2, ensure_ascii=False))
            
            if response.status_code == 200:
                print("\n✅ 测试通过！终点结论包生成成功")
                return True
            else:
                print(f"\n❌ 测试失败！状态码: {response.status_code}")
                return False
                
        except Exception as e:
            print(f"\n❌ 测试失败！错误: {str(e)}")
            import traceback
            traceback.print_exc()
            return False

async def test_conclusion_package_with_mock_data():
    """使用模拟数据测试（不依赖诊断服务）"""
    print(f"\n{'='*60}")
    print(f"测试2: 使用模拟数据测试终点结论包生成")
    print(f"{'='*60}\n")
    
    # 模拟CDP数据（包含空ddx的情况）
    mock_cdp_data = {
        "id": "test_cdp_001",
        "patientState": {
            "symptoms": [
                {
                    "name": "腹痛",
                    "cui": "C0000737",
                    "duration": "3天",
                    "severity": "severe",
                    "trigger": "活动后",
                    "location": "上腹部"
                }
            ],
            "signs": {},
            "examinations": []
        },
        "ddx": [],  # 空列表，测试修复后的兼容性
        "evidenceGraph": {},
        "workupPlan": [],
        "managementPlan": [],
        "triage": {}
    }
    
    async with httpx.AsyncClient(timeout=30.0) as client:
        try:
            # 直接测试构建器（需要修改服务以支持直接传入数据）
            # 这里我们测试通过诊断服务获取CDP，但使用模拟数据
            print("注意：此测试需要修改服务代码以支持直接传入CDP数据")
            print("或者需要先创建一个测试CDP")
            print("\n跳过此测试，使用真实CDP测试...")
            return True
                
        except Exception as e:
            print(f"\n❌ 测试失败！错误: {str(e)}")
            return False

async def test_evidence_chain(cdp_id: str):
    """测试证据链生成"""
    print(f"\n{'='*60}")
    print(f"测试3: 测试证据链生成")
    print(f"CDP ID: {cdp_id}")
    print(f"{'='*60}\n")
    
    async with httpx.AsyncClient(timeout=30.0) as client:
        try:
            url = f"{EXPLANATION_SERVICE_URL}/api/v1/explain/evidence-chain"
            request_data = {
                "cdpId": cdp_id
            }
            
            print(f"请求URL: {url}")
            print("\n发送请求...")
            
            response = await client.post(url, json=request_data)
            
            print(f"\n响应状态码: {response.status_code}")
            if response.status_code == 200:
                result = response.json()
                print(f"证据链数据:")
                print(json.dumps(result, indent=2, ensure_ascii=False))
                print("\n✅ 测试通过！证据链生成成功")
                return True
            else:
                print(f"响应内容: {response.text}")
                print(f"\n❌ 测试失败！状态码: {response.status_code}")
                return False
                
        except Exception as e:
            print(f"\n❌ 测试失败！错误: {str(e)}")
            import traceback
            traceback.print_exc()
            return False

async def test_full_explain(cdp_id: str):
    """测试完整解释生成"""
    print(f"\n{'='*60}")
    print(f"测试4: 测试完整解释生成")
    print(f"CDP ID: {cdp_id}")
    print(f"{'='*60}\n")
    
    async with httpx.AsyncClient(timeout=30.0) as client:
        try:
            url = f"{EXPLANATION_SERVICE_URL}/api/v1/explain"
            request_data = {
                "cdpId": cdp_id
            }
            
            print(f"请求URL: {url}")
            print("\n发送请求...")
            
            response = await client.post(url, json=request_data)
            
            print(f"\n响应状态码: {response.status_code}")
            if response.status_code == 200:
                result = response.json()
                print(f"完整解释数据（部分）:")
                # 只打印关键字段，避免输出过长
                if "conclusionPackage" in result:
                    print(f"  结论包: ✅")
                if "evidenceChain" in result:
                    print(f"  证据链: ✅")
                if "reasoningPaths" in result:
                    print(f"  推理路径: ✅")
                if "naturalLanguageExplanation" in result:
                    print(f"  自然语言解释: ✅")
                print("\n✅ 测试通过！完整解释生成成功")
                return True
            else:
                print(f"响应内容: {response.text}")
                print(f"\n❌ 测试失败！状态码: {response.status_code}")
                return False
                
        except Exception as e:
            print(f"\n❌ 测试失败！错误: {str(e)}")
            import traceback
            traceback.print_exc()
            return False

async def check_service_health():
    """检查服务健康状态"""
    print(f"\n{'='*60}")
    print(f"检查服务健康状态")
    print(f"{'='*60}\n")
    
    async with httpx.AsyncClient(timeout=10.0) as client:
        try:
            # 检查解释服务
            response = await client.get(f"{EXPLANATION_SERVICE_URL}/health")
            if response.status_code == 200:
                print(f"✅ 解释服务健康: {response.json()}")
            else:
                print(f"❌ 解释服务不健康: {response.status_code}")
                return False
                
            # 检查诊断服务（用于获取CDP）
            response = await client.get(f"{DIAGNOSIS_SERVICE_URL}/actuator/health")
            if response.status_code == 200:
                print(f"✅ 诊断服务健康: {response.json()}")
            else:
                print(f"⚠️  诊断服务健康检查失败: {response.status_code}（可能不影响测试）")
                
            return True
        except Exception as e:
            print(f"❌ 服务健康检查失败: {str(e)}")
            return False

async def main():
    """主测试函数"""
    print("\n" + "="*60)
    print("解释服务独立测试")
    print("="*60)
    
    # 1. 检查服务健康状态
    if not await check_service_health():
        print("\n⚠️  服务健康检查失败，但继续测试...")
    
    # 2. 使用您提供的CDP ID进行测试
    # 从CDP.txt中获取的CDP ID
    test_cdp_id = "cdp_d092ba1e2c4f489b9809e74445cd51e3"
    
    print(f"\n使用CDP ID: {test_cdp_id}")
    print("（您可以从CDP.txt中获取其他CDP ID进行测试）")
    
    # 3. 运行测试
    results = []
    
    # 测试1: 终点结论包生成（最重要的测试）
    result1 = await test_conclusion_package_with_real_cdp(test_cdp_id)
    results.append(("终点结论包生成", result1))
    
    # 测试2: 证据链生成
    result2 = await test_evidence_chain(test_cdp_id)
    results.append(("证据链生成", result2))
    
    # 测试3: 完整解释生成
    result3 = await test_full_explain(test_cdp_id)
    results.append(("完整解释生成", result3))
    
    # 4. 输出测试总结
    print(f"\n{'='*60}")
    print("测试总结")
    print(f"{'='*60}\n")
    
    for test_name, result in results:
        status = "✅ 通过" if result else "❌ 失败"
        print(f"{test_name}: {status}")
    
    all_passed = all(result for _, result in results)
    if all_passed:
        print("\n🎉 所有测试通过！解释服务工作正常。")
    else:
        print("\n⚠️  部分测试失败，请检查错误信息。")
    
    return all_passed

if __name__ == "__main__":
    # 运行测试
    success = asyncio.run(main())
    exit(0 if success else 1)

