import axios from 'axios'
import router from '../router'

export const SESSION_KEY = 'joincom_admin_session'
export type AdminSession = { token:string; userId:number; username:string; role:string; scopeType:string; scopeId:number|null }
export function session():AdminSession|null { try{return JSON.parse(sessionStorage.getItem(SESSION_KEY)||'null')}catch{return null} }
export function saveSession(value:AdminSession){sessionStorage.setItem(SESSION_KEY,JSON.stringify(value))}
export function clearSession(){sessionStorage.removeItem(SESSION_KEY)}

const http:any=axios.create({baseURL:'/api/admin',timeout:30000})
http.interceptors.request.use((config:any)=>{const token=session()?.token;if(token)config.headers.Authorization=`Bearer ${token}`;return config})
http.interceptors.response.use((response:any)=>{const body=response.data;if(body?.code===1)return body.data;return Promise.reject(new Error(body?.message||'请求失败'))},(error:any)=>{if(error.response?.status===401){clearSession();router.replace('/login')}return Promise.reject(new Error(error.response?.data?.message||error.message||'网络异常'))})
export default http
