import { defineStore } from 'pinia'
import { ref } from 'vue'


export const useUserStore = defineStore('user', () => {


  // token
  const token = ref(
    localStorage.getItem('token') || ''
  )


  // 用户信息
  const savedUserInfo = localStorage.getItem('userInfo')

  const userInfo = ref<any>(
    savedUserInfo 
      ? JSON.parse(savedUserInfo) 
      : null
  )


  console.log(
    '[DEBUG] userStore init:',
    savedUserInfo
  )



  // 保存token
  const setToken = (newToken:string)=>{

    token.value = newToken

    localStorage.setItem(
      'token',
      newToken
    )

  }




  // 保存用户信息
  const setUserInfo = (info:any)=>{


    console.log(
      '[DEBUG] setUserInfo:',
      JSON.stringify(info)
    )


    userInfo.value = info



    // 保存角色数组
    if(info?.roles){

      localStorage.setItem(
        'roles',
        JSON.stringify(info.roles)
      )

    }



    localStorage.setItem(
      'userInfo',
      JSON.stringify(info)
    )


  }




  //退出登录
  const logout = ()=>{


    token.value=''

    userInfo.value=null



    localStorage.removeItem('token')

    localStorage.removeItem('roles')

    localStorage.removeItem('userInfo')


    localStorage.removeItem(
      'tutor_current_session'
    )

    localStorage.removeItem(
      'tutor_current_messages'
    )


  }



  return {

    token,

    userInfo,

    setToken,

    setUserInfo,

    logout

  }


})