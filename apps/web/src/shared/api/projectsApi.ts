import { requestJson } from './apiError'
import { getAccessToken } from '../auth/authSession'

export type Project = {
  id: string
  organizationId: string
  name: string
  description: string | null
  status: 'ACTIVE' | 'ARCHIVED'
  createdBy: string
  createdAt: string
  updatedAt: string
}
export type ProjectList = { items: Project[]; page: number; size: number; total: number; totalPages: number }

const base = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'
function headers() { return { Accept: 'application/json', Authorization: `Bearer ${getAccessToken() ?? ''}` } }

export function listProjects(page = 0, size = 25, status?: Project['status']): Promise<ProjectList> {
  const query = new URLSearchParams({ page: String(page), size: String(size) })
  if (status) query.set('status', status)
  return requestJson(`${base}/projects?${query.toString()}`, { headers: headers() }, 'Projects request')
}

export function createProject(name: string, description: string): Promise<Project> {
  return requestJson(`${base}/projects`, {
    method: 'POST', headers: { ...headers(), 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, description: description || null }),
  }, 'Project creation request')
}

export function getProject(projectId: string): Promise<Project> {
  return requestJson(`${base}/projects/${projectId}`, { headers: headers() }, 'Project request')
}

export function updateProject(projectId: string, name: string, description: string | null): Promise<Project> {
  return requestJson(`${base}/projects/${projectId}`, {
    method: 'PATCH', headers: { ...headers(), 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, description }),
  }, 'Project update request')
}

export function archiveProject(projectId: string): Promise<Project> {
  return requestJson(`${base}/projects/${projectId}/archive`, { method: 'POST', headers: headers() }, 'Project archive request')
}

export function restoreProject(projectId: string): Promise<Project> {
  return requestJson(`${base}/projects/${projectId}/restore`, { method: 'POST', headers: headers() }, 'Project restore request')
}
