export type ApiErrorKind = 'http' | 'network'

export class ApiError extends Error {
  readonly kind: ApiErrorKind
  readonly status: number | null

  private constructor(message: string, kind: ApiErrorKind, status: number | null) {
    super(message)
    this.name = 'ApiError'
    this.kind = kind
    this.status = status
  }

  static http(status: number, message: string): ApiError {
    return new ApiError(message, 'http', status)
  }

  static network(message = 'Unable to connect to the backend.'): ApiError {
    return new ApiError(message, 'network', null)
  }
}

export function isUnauthorizedError(error: unknown): boolean {
  return error instanceof ApiError && error.kind === 'http' && error.status === 401
}

export async function requestJson<T>(input: RequestInfo | URL, init: RequestInit | undefined, operation: string): Promise<T> {
  let response: Response
  try {
    response = await fetch(input, init)
  } catch {
    throw ApiError.network()
  }

  return readJsonResponse<T>(response, operation)
}

export async function readJsonResponse<T>(response: Response, operation: string): Promise<T> {
  let body: unknown
  try {
    const text = await response.text()
    body = text ? JSON.parse(text) : undefined
  } catch {
    body = undefined
  }

  if (!response.ok) {
    const message = typeof body === 'object' && body !== null && 'message' in body && typeof body.message === 'string'
      ? body.message
      : `${operation} failed with HTTP ${response.status}.`
    throw ApiError.http(response.status, message)
  }
  return body as T
}

export async function request(input: RequestInfo | URL, init: RequestInit | undefined, operation: string): Promise<Response> {
  try {
    const response = await fetch(input, init)
    if (!response.ok) {
      throw ApiError.http(response.status, `${operation} failed with HTTP ${response.status}.`)
    }
    return response
  } catch (error) {
    if (error instanceof ApiError) throw error
    throw ApiError.network()
  }
}
